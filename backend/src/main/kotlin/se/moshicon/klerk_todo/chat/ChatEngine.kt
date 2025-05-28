package se.moshicon.klerk_todo.chat

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.sse.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import org.slf4j.LoggerFactory
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.*
import com.openai.models.ChatModel
import com.openai.models.FunctionDefinition
import se.moshicon.klerk_todo.McpClientConfig
import java.time.Duration


class ChatEngine(
    private val mcpServerUrl: Url,
    private val llmServerUrl: Url
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val mcp: Client = Client(clientInfo = Implementation(name = "mcp-todo-chat-client", version = "1.0.0"))
    private val httpClient = HttpClient(CIO) {
        install(SSE)
    }
    private var transport: SseClientTransport? = null
    private var isConnected = false
    private var tools: List<Tool> = emptyList()

    // OpenAI client for LLM communication
    private val openAIClient: OpenAIClient by lazy {
        OpenAIOkHttpClient.builder()
            .apiKey("dummy-api-key") // Ollama doesn't require a real API key
            .baseUrl(llmServerUrl.toString())
            .timeout(Duration.ofSeconds(60))
            .build()
    }

    suspend fun initMcpServerConnection() {
        if (isConnected) {
            logger.info("MCP server connection already established")
            return
        }

        val maxRetries = 5
        val retryDelayMs = 1000L

        repeat(maxRetries) { attempt ->
            try {
                logger.info("Attempting to connect to MCP server at ${mcpServerUrl} (attempt ${attempt + 1}/$maxRetries)")

                // Create a new transport for each attempt
                transport = SseClientTransport(httpClient, mcpServerUrl.toString())
                mcp.connect(transport!!)

                val toolsResult = mcp.listTools()
                tools = toolsResult?.tools ?: emptyList()
                isConnected = true
                logger.info("Successfully connected to MCP server and fetched ${tools.size} tools")
                return
            } catch (e: Exception) {
                logger.warn("Failed to connect to MCP server (attempt ${attempt + 1}/$maxRetries): ${e.message}")
                transport = null // Reset transport for next attempt

                if (attempt < maxRetries - 1) {
                    delay(retryDelayMs * (attempt + 1)) // Exponential backoff
                } else {
                    throw e
                }
            }
        }
    }

    /**
     * Converts MCP tools to OpenAI function format for the LLM
     */
    private fun convertMcpToolsToOpenAiFunctions(): List<ChatCompletionTool> {
        return tools.map { tool ->
            ChatCompletionTool.builder()
                .function(
                    FunctionDefinition.builder()
                        .name(tool.name)
                        .description(tool.description ?: "")
                        .build()
                )
                .build()
        }
    }

    /**
     * Converts JsonObject to Map for the OpenAI library
     */
    private fun convertJsonObjectToMap(jsonObject: JsonObject): Map<String, Any> {
        return jsonObject.entries.associate { (key, value) ->
            key to when (value) {
                is JsonPrimitive -> {
                    when {
                        value.isString -> value.content
                        else -> value.content
                    }
                }
                is JsonObject -> convertJsonObjectToMap(value)
                else -> value.toString()
            }
        }
    }

    /**
     * Converts chat session history to OpenAI message format
     */
    private fun convertChatHistoryToOpenAiMessages(chatSession: ChatSession): MutableList<ChatCompletionMessageParam> {
        val messages = mutableListOf<ChatCompletionMessageParam>()

        // Add system message
        messages.add(
            ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
                .content("""You are a helpful assistant that can manage TODO items.
                    You have access to tools that allow you to create, list, and manage TODOs.
                    Use these tools when the user asks about TODO management."""
                )
                .build()
            )
        )

        // Convert chat history to alternating user/assistant messages
        chatSession.getHistory().forEachIndexed { index, msg ->
            if (index % 2 == 0) {
                messages.add(ChatCompletionUserMessageParam.builder()
                    .content(msg.content)
                    .build() as ChatCompletionMessageParam)
            } else {
                messages.add(ChatCompletionAssistantMessageParam.builder()
                    .content(msg.content)
                    .build() as ChatCompletionMessageParam)
            }
        }

        return messages
    }

    private suspend fun executeMcpTool(toolName: String, arguments: Map<String, Any>): CallToolResult {
        if (!isConnected) {
            throw IllegalStateException("MCP server not connected")
        }

        // Convert arguments to JsonObject
        val jsonArguments = JsonObject(arguments.mapValues { (_, value) ->
            when (value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString())
            }
        })

        val request = CallToolRequest(name = toolName, arguments = jsonArguments)
        val result = mcp.callTool(request)
        return result as? CallToolResult ?: throw RuntimeException("Tool call failed: $result")
    }

    suspend fun handleChatMessage(chatSession: ChatSession, message: se.moshicon.klerk_todo.chat.ChatMessage): se.moshicon.klerk_todo.chat.ChatMessage {
        chatSession.addMessage(message)

        try {
            // Ensure MCP connection is established
            if (!isConnected) {
                initMcpServerConnection()
            }

            // Convert chat history to OpenAI format
            val openAiMessages = convertChatHistoryToOpenAiMessages(chatSession)

            // Get available functions from MCP tools
            val functions = convertMcpToolsToOpenAiFunctions()

            // Create chat completion request
            val requestBuilder = ChatCompletionCreateParams.builder()
                .model(ChatModel.of(McpClientConfig.llmModel))
                .messages(openAiMessages)

            // Add tools if available
            if (functions.isNotEmpty()) {
                requestBuilder.tools(functions)
            }

            val chatCompletionRequest = requestBuilder.build()

            // Call LLM
            val completion = openAIClient.chat().completions().create(chatCompletionRequest)
            val choice = completion.choices().firstOrNull()
                ?: throw RuntimeException("No response from LLM")

            val assistantMessage = choice.message()

            // Check if LLM wants to call a function
            val toolCalls = assistantMessage.toolCalls()
            if (toolCalls.isPresent && toolCalls.get().isNotEmpty()) {
                val toolCallsList = toolCalls.get()
                logger.info("LLM requested ${toolCallsList.size} tool calls")

                try {
                    val updatedMessages = openAiMessages.toMutableList()

                    // Add the assistant message with tool calls
                    updatedMessages.add(ChatCompletionAssistantMessageParam.builder()
                        .content(assistantMessage.content().orElse(""))
                        .toolCalls(toolCallsList)
                        .build() as ChatCompletionMessageParam)

                    // Execute each tool call and add results
                    for (toolCall in toolCallsList) {
                        val function = toolCall.function()
                        logger.info("Executing tool: ${function.name()} with arguments: ${function.arguments()}")

                        // Parse function arguments
                        val arguments = com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(function.arguments(), Map::class.java) as Map<String, Any>

                        // Execute MCP tool
                        val toolResult = executeMcpTool(function.name(), arguments)

                        // Add tool result message
                        updatedMessages.add(ChatCompletionToolMessageParam.builder()
                            .toolCallId(toolCall.id())
                            .content(toolResult.content.joinToString("\n") { it.toString() })
                            .build() as ChatCompletionMessageParam)
                    }

                    // Get final response from LLM
                    val finalRequest = ChatCompletionCreateParams.builder()
                        .model(ChatModel.of(McpClientConfig.llmModel))
                        .messages(updatedMessages)
                        .build()

                    val finalCompletion = openAIClient.chat().completions().create(finalRequest)
                    val finalChoice = finalCompletion.choices().firstOrNull()
                        ?: throw RuntimeException("No final response from LLM")

                    val responseMessage = se.moshicon.klerk_todo.chat.ChatMessage(
                        content = finalChoice.message().content().orElse("I executed the requested action.")
                    )
                    chatSession.addMessage(responseMessage)
                    return responseMessage

                } catch (e: Exception) {
                    logger.error("Error executing tool calls: ${e.message}", e)
                    val errorMessage = se.moshicon.klerk_todo.chat.ChatMessage(
                        content = "I encountered an error while trying to execute that action: ${e.message}"
                    )
                    chatSession.addMessage(errorMessage)
                    return errorMessage
                }
            } else {
                // Direct response from LLM without tool calls
                val responseMessage = se.moshicon.klerk_todo.chat.ChatMessage(
                    content = assistantMessage.content().orElse("I'm not sure how to respond to that.")
                )
                chatSession.addMessage(responseMessage)
                return responseMessage
            }

        } catch (e: Exception) {
            logger.error("Error in LLM chat processing: ${e.message}", e)
            val fallbackMessage = se.moshicon.klerk_todo.chat.ChatMessage(
                content = "I'm sorry, I encountered an error while processing your message. Please try again."
            )
            chatSession.addMessage(fallbackMessage)
            return fallbackMessage
        }
    }
}