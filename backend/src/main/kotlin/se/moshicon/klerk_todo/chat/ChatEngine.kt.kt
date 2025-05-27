package se.moshicon.klerk_todo.chat

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.sse.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import org.slf4j.LoggerFactory
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import kotlinx.coroutines.delay


class ChatEngine(
    private val mcpServerUrl: Url,
    private val llmServerUrl: Url
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val mcp: Client = Client(clientInfo = Implementation(name = "mcp-todo-chat-client", version = "1.0.0"))
    val httpClient = HttpClient(CIO) {
        install(SSE)
    }
    private var transport: SseClientTransport? = null
    private var isConnected = false

//    init {
//        logger.info(mcp.toString())
//        logger.info("Chat engine initialized")
//    }

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
                toolsResult?.tools?.forEach {
                    logger.info("Tool: ${it.name}")
                }

                isConnected = true
                logger.info("Successfully connected to MCP server")
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

    fun handleChatMessage(chatSession: ChatSession, message: ChatMessage): ChatMessage {
        chatSession.addMessage(message)
        val responseMessage = ChatMessage(
            content = "You said ${message.content}."
        )
        chatSession.addMessage(responseMessage)
        return responseMessage
    }
}