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
import com.openai.models.FunctionParameters
import com.openai.core.JsonValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

import se.moshicon.klerk_todo.McpClientConfig
import java.time.Duration


class ChatEngine(
    private val mcpServerUrl: Url,
    private val llmServerUrl: Url,
    private val llmApiKey: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val llmDebugLogger = LoggerFactory.getLogger("se.moshicon.klerk_todo.chat.LLMDebugLogger")
    private val mcp: Client = Client(clientInfo = Implementation(name = "mcp-todo-chat-client", version = "1.0.0"))
    private val httpClient = HttpClient(CIO) {
        install(SSE)
    }
    private var transport: SseClientTransport? = null
    private var isConnected = false
    private var tools: List<Tool> = emptyList()

    // Check if LLM debug logging is enabled via environment variable
    private val isLlmDebugEnabled = System.getenv("LLM_LOGFILE") != null

    // JSON mapper for pretty printing debug logs
    private val jsonMapper = ObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    // OpenAI client for LLM communication
    private val openAIClient: OpenAIClient by lazy {
        OpenAIOkHttpClient.builder()
            .apiKey(llmApiKey)
            .baseUrl(llmServerUrl.toString())
            .timeout(Duration.ofSeconds(60))
            .build()
    }

    /**
     * Logs LLM request details for debugging
     */
    private fun logLlmRequest(requestType: String, request: ChatCompletionCreateParams, sessionId: String) {
        if (!isLlmDebugEnabled || !llmDebugLogger.isDebugEnabled) return

        try {
            llmDebugLogger.debug("=== LLM REQUEST ($requestType) ===")
            llmDebugLogger.debug("Session ID: $sessionId")
            llmDebugLogger.debug("Model: ${request.model()}")
            llmDebugLogger.debug("Messages:")

            request.messages().forEachIndexed { index, message ->
                llmDebugLogger.debug("  Message $index:")
                when {
                    message.isSystem() -> {
                        llmDebugLogger.debug("    Role: system")
                        llmDebugLogger.debug("    Content: ${message.asSystem().content()}")
                    }
                    message.isUser() -> {
                        llmDebugLogger.debug("    Role: user")
                        llmDebugLogger.debug("    Content: ${message.asUser().content().text().orElse("")}")
                    }
                    message.isAssistant() -> {
                        llmDebugLogger.debug("    Role: assistant")
                        val assistantContent = message.asAssistant().content()
                        if (assistantContent.isPresent) {
                            llmDebugLogger.debug("    Content: ${assistantContent.get()}")
                        } else {
                            llmDebugLogger.debug("    Content: (empty)")
                        }
                        if (message.asAssistant().toolCalls().isPresent) {
                            llmDebugLogger.debug("    Tool Calls:")
                            message.asAssistant().toolCalls().get().forEachIndexed { toolIndex, toolCall ->
                                llmDebugLogger.debug("      Tool Call $toolIndex:")
                                llmDebugLogger.debug("        ID: ${toolCall.id()}")
                                llmDebugLogger.debug("        Function: ${toolCall.function().name()}")
                                llmDebugLogger.debug("        Arguments: ${toolCall.function().arguments()}")
                            }
                        }
                    }
                    message.isTool() -> {
                        llmDebugLogger.debug("    Role: tool")
                        llmDebugLogger.debug("    Tool Call ID: ${message.asTool().toolCallId()}")
                        llmDebugLogger.debug("    Content: ${message.asTool().content()}")
                    }
                }
            }

            if (request.tools().isPresent && request.tools().get().isNotEmpty()) {
                llmDebugLogger.debug("Available Tools:")
                request.tools().get().forEachIndexed { index, tool ->
                    llmDebugLogger.debug("  Tool $index:")
                    llmDebugLogger.debug("    Name: ${tool.function().name()}")
                    llmDebugLogger.debug("    Description: ${tool.function().description().orElse("")}")
                    if (tool.function().parameters().isPresent) {
                        llmDebugLogger.debug("    Parameters: (present)")
                    } else {
                        llmDebugLogger.debug("    Parameters: (none)")
                    }
                }
            }

            llmDebugLogger.debug("=== END LLM REQUEST ===")
        } catch (e: Exception) {
            llmDebugLogger.warn("Failed to log LLM request: ${e.message}")
        }
    }

    /**
     * Logs LLM response details for debugging
     */
    private fun logLlmResponse(requestType: String, response: ChatCompletion, sessionId: String) {
        if (!isLlmDebugEnabled || !llmDebugLogger.isDebugEnabled) return

        try {
            llmDebugLogger.debug("=== LLM RESPONSE ($requestType) ===")
            llmDebugLogger.debug("Session ID: $sessionId")
            llmDebugLogger.debug("Model: ${response.model()}")
            llmDebugLogger.debug("Usage:")
            response.usage().ifPresent { usage ->
                llmDebugLogger.debug("  Prompt tokens: ${usage.promptTokens()}")
                llmDebugLogger.debug("  Completion tokens: ${usage.completionTokens()}")
                llmDebugLogger.debug("  Total tokens: ${usage.totalTokens()}")
            }

            llmDebugLogger.debug("Choices:")
            response.choices().forEachIndexed { index, choice ->
                llmDebugLogger.debug("  Choice $index:")
                llmDebugLogger.debug("    Finish reason: ${choice.finishReason()}")
                llmDebugLogger.debug("    Message:")
                val message = choice.message()
                llmDebugLogger.debug("      Role: assistant")
                llmDebugLogger.debug("      Content: ${message.content().orElse("")}")

                if (message.toolCalls().isPresent && message.toolCalls().get().isNotEmpty()) {
                    llmDebugLogger.debug("      Tool Calls:")
                    message.toolCalls().get().forEachIndexed { toolIndex, toolCall ->
                        llmDebugLogger.debug("        Tool Call $toolIndex:")
                        llmDebugLogger.debug("          ID: ${toolCall.id()}")
                        llmDebugLogger.debug("          Function: ${toolCall.function().name()}")
                        llmDebugLogger.debug("          Arguments: ${toolCall.function().arguments()}")
                    }
                }
            }

            llmDebugLogger.debug("=== END LLM RESPONSE ===")
        } catch (e: Exception) {
            llmDebugLogger.warn("Failed to log LLM response: ${e.message}")
        }
    }

    /**
     * Logs tool execution details for debugging
     */
    private fun logToolExecution(toolName: String, arguments: Map<String, Any>, result: CallToolResult, sessionId: String) {
        if (!isLlmDebugEnabled || !llmDebugLogger.isDebugEnabled) return

        try {
            llmDebugLogger.debug("=== TOOL EXECUTION ===")
            llmDebugLogger.debug("Session ID: $sessionId")
            llmDebugLogger.debug("Tool Name: $toolName")
            llmDebugLogger.debug("Arguments: ${jsonMapper.writeValueAsString(arguments)}")
            llmDebugLogger.debug("Result:")
            result.content.forEachIndexed { index, content ->
                llmDebugLogger.debug("  Content $index: $content")
            }
            llmDebugLogger.debug("=== END TOOL EXECUTION ===")
        } catch (e: Exception) {
            llmDebugLogger.warn("Failed to log tool execution: ${e.message}")
        }
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
            val functionBuilder = FunctionDefinition.builder()
                .name(tool.name)
                .description(tool.description ?: "")

            // Add parameters schema if available
            tool.inputSchema.let { inputSchema: Tool.Input ->
                val parametersSchema = convertMcpInputSchemaToOpenAiParameters(inputSchema)
                val functionParametersBuilder = FunctionParameters.builder()

                // Add each property from the schema
                parametersSchema.forEach { (key, value) ->
                    functionParametersBuilder.putAdditionalProperty(key, JsonValue.from(value))
                }

                functionBuilder.parameters(functionParametersBuilder.build())
            }

            ChatCompletionTool.builder()
                .function(functionBuilder.build())
                .build()
        }
    }

    /**
     * Converts MCP Tool.Input schema to OpenAI function parameters format
     */
    private fun convertMcpInputSchemaToOpenAiParameters(inputSchema: Tool.Input): Map<String, Any> {
        val parametersMap = mutableMapOf<String, Any>()

        // Set the type to "object" as OpenAI expects
        parametersMap["type"] = "object"

        // Convert properties from JsonObject to Map
        val properties = mutableMapOf<String, Any>()
        inputSchema.properties.forEach { (key, value) ->
            properties[key] = convertJsonObjectToMap(value as JsonObject)
        }
        parametersMap["properties"] = properties

        // Add required parameters if any
        inputSchema.required?.let { requiredList ->
            if (requiredList.isNotEmpty()) {
                parametersMap["required"] = requiredList
            }
        }

        return parametersMap
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

        // Add system message if no internal messages exist yet
        if (chatSession.getInternalHistory().isEmpty()) {
            messages.add(
                ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
                    .content("""You are a helpful assistant that can manage TODO items in a web based TODO application.
                        You have access to tools (function invocations) to list and manipulate the TODOs. Use the tools are you see fit to help the user.
                        A normal user will only see its own TODOs. But a user that is a member of the admin group can see all. Some users are guest and can only created a limited amount of TODO items.
                        The user is logged in as the username "${chatSession.userName}" and userID "${chatSession.userId}".
                        The TODO objects is associated with the userID.
                    """
                    )
                    .build()
                )
            )
        }

        // Add previous chat messages
        messages.addAll(chatSession.getInternalHistory())

        return messages
    }

    private suspend fun executeMcpTool(toolName: String, arguments: Map<String, Any>, sessionId: String): CallToolResult {
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
        val toolResult = result as? CallToolResult ?: throw RuntimeException("Tool call failed: $result")

        // Log the tool execution
        logToolExecution(toolName, arguments, toolResult, sessionId)

        return toolResult
    }

    suspend fun handleChatMessage(chatSession: ChatSession, message: ChatMessage): BrowserChatMessage {
        // Add user message to internal message list
        val userInternalMessage = InternalChatMessage(
            messageParam = ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                    .content(message.content)
                    .build()
            ),
            timestamp = message.timestamp
        )

        try {
            // Ensure MCP connection is established
            if (!isConnected) {
                initMcpServerConnection()
            }

            // Convert chat history to OpenAI format
            val openAiMessages = convertChatHistoryToOpenAiMessages(chatSession)
            // Add the current user message to the messages for the LLM request
            openAiMessages.add(userInternalMessage.messageParam)
            // Also add to internal history for future conversations
            chatSession.addInternalMessage(userInternalMessage)

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

            // Log the LLM request
            logLlmRequest("INITIAL", chatCompletionRequest, chatSession.sessionId)

            // Call LLM
            val completion = openAIClient.chat().completions().create(chatCompletionRequest)

            // Log the LLM response
            logLlmResponse("INITIAL", completion, chatSession.sessionId)

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
                    val assistantWithToolCallsParam = ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder()
                            .content(assistantMessage.content().orElse(""))
                            .toolCalls(toolCallsList)
                            .build()
                    )
                    updatedMessages.add(assistantWithToolCallsParam)

                    // Store the assistant message with tool calls in internal history
                    chatSession.addInternalMessage(InternalChatMessage(messageParam = assistantWithToolCallsParam))

                    // Execute each tool call and add results
                    for (toolCall in toolCallsList) {
                        val function = toolCall.function()
                        logger.info("Executing tool: ${function.name()} with arguments: ${function.arguments()}")

                        // Parse function arguments
                        val arguments = com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(function.arguments(), Map::class.java) as Map<String, Any>

                        // Execute MCP tool
                        val toolResult = executeMcpTool(function.name(), arguments, chatSession.sessionId)

                        // Add tool result message
                        val toolResultParam = ChatCompletionMessageParam.ofTool(
                            ChatCompletionToolMessageParam.builder()
                                .toolCallId(toolCall.id())
                                .content(toolResult.content.joinToString("\n") { it.toString() })
                                .build()
                        )
                        updatedMessages.add(toolResultParam)

                        // Store the tool result in internal history
                        chatSession.addInternalMessage(InternalChatMessage(messageParam = toolResultParam))
                    }

                    // Get final response from LLM
                    val finalRequest = ChatCompletionCreateParams.builder()
                        .model(ChatModel.of(McpClientConfig.llmModel))
                        .messages(updatedMessages)
                        .build()

                    // Log the final LLM request
                    logLlmRequest("FINAL", finalRequest, chatSession.sessionId)

                    val finalCompletion = openAIClient.chat().completions().create(finalRequest)

                    // Log the final LLM response
                    logLlmResponse("FINAL", finalCompletion, chatSession.sessionId)

                    val finalChoice = finalCompletion.choices().firstOrNull()
                        ?: throw RuntimeException("No final response from LLM")

                    val finalResponseContent = finalChoice.message().content().orElse("I executed the requested action.")

                    // Store the final assistant response in internal history
                    val finalAssistantParam = ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder()
                            .content(finalResponseContent)
                            .build()
                    )
                    chatSession.addInternalMessage(InternalChatMessage(messageParam = finalAssistantParam))

                    // Return the browser message by converting from the last internal message
                    return chatSession.getHistory().lastOrNull() ?: BrowserChatMessage(
                        content = finalResponseContent,
                        sender = MessageSender.ASSISTANT
                    )

                } catch (e: Exception) {
                    logger.error("Error executing tool calls: ${e.message}", e)
                    return BrowserChatMessage(
                        content = "I encountered an error while trying to execute that action: ${e.message}",
                        sender = MessageSender.ASSISTANT
                    )
                }
            } else {
                // Direct response from LLM without tool calls
                val responseContent = assistantMessage.content().orElse("I'm not sure how to respond to that.")

                // Store the assistant response in internal history
                val assistantParam = ChatCompletionMessageParam.ofAssistant(
                    ChatCompletionAssistantMessageParam.builder()
                        .content(responseContent)
                        .build()
                )
                chatSession.addInternalMessage(InternalChatMessage(messageParam = assistantParam))

                // Return the browser message by converting from the last internal message
                return chatSession.getHistory().lastOrNull() ?: BrowserChatMessage(
                    content = responseContent,
                    sender = MessageSender.ASSISTANT
                )
            }

        } catch (e: Exception) {
            logger.error("Error in LLM chat processing: ${e.message}", e)
            return BrowserChatMessage(
                content = "I'm sorry, I encountered an error while processing your message. Please try again.",
                sender = MessageSender.ASSISTANT
            )
        }
    }
}