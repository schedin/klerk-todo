package se.moshicon.klerk_todo.chat

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import org.slf4j.LoggerFactory
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport


class ChatEngine(
    private val mcpServerUrl: Url,
    private val llmServerUrl: Url
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val mcp: Client = Client(clientInfo = Implementation(name = "mcp-todo-chat-client", version = "1.0.0"))
    val httpClient = HttpClient(CIO)
    val transport = SseClientTransport(httpClient, mcpServerUrl.toString())

//    init {
//        logger.info(mcp.toString())
//        logger.info("Chat engine initialized")
//    }

    suspend fun initMcpServerConnection() {
        mcp.connect(transport)
        val toolsResult = mcp.listTools()
        toolsResult?.tools?.forEach {
            logger.info("Tool: ${it.name}")
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