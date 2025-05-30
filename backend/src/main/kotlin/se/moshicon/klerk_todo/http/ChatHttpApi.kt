package se.moshicon.klerk_todo.http

import dev.klerkframework.klerk.Klerk
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import se.moshicon.klerk_todo.Ctx
import se.moshicon.klerk_todo.Data
import se.moshicon.klerk_todo.McpServerConfig
import se.moshicon.klerk_todo.chat.*

private val logger = LoggerFactory.getLogger("se.moshicon.klerk_todo.http.ChatHttpApi")

fun registerChatRoutes(klerk: Klerk<Ctx, Data>, mcpServerConfig: McpServerConfig): Route.() -> Unit = {
    get("/history") { getChatHistory(call) }
    post("/message") { addNewChatMessage(call) }
    delete("/history") { clearChatHistory(call) }
}

private suspend fun getChatHistory(call: ApplicationCall) {
    val userName = getSubjectFromCall(call) ?: return
    try {
        val session = ChatSessionManager.getSessionByUserName(userName)
        val messages = session?.getHistory() ?: emptyList()
        call.respond(HttpStatusCode.OK, ChatHistoryResponse(messages))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get history: ${e.message}"))
    }
}

private suspend fun addNewChatMessage(call: ApplicationCall) {
    val userName = getSubjectFromCall(call) ?: return
    try {
        val request = call.receive<ChatMessageRequest>()

        // Validate message content
        if (request.content.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Message content cannot be empty"))
            return
        }

        val chatMessage = ChatMessage(
            content = request.content.trim()
        )

        val chatSession = ChatSessionManager.getOrCreateSession(userName)

        // Extract JWT token from Authorization header and store it in the session
        val authHeader = call.request.headers["Authorization"]
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7).trim()
            chatSession.updateAuthToken(token)
        }
        val responseMessage = ChatSessionManager.chatEngine.handleChatMessage(chatSession, chatMessage)
        logger.info("Response message: $responseMessage")
        call.respond(HttpStatusCode.OK, ChatMessageResponse(responseMessage))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to send message: ${e.message}"))
    }
}

private suspend fun clearChatHistory(call: ApplicationCall) {
    val userName = getSubjectFromCall(call) ?: return

    try {
        val chatSession = ChatSessionManager.getSessionByUserName(userName)
        chatSession?.clearHistory()
        call.respond(HttpStatusCode.OK, mapOf("message" to "Chat history cleared successfully"))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to clear history: ${e.message}"))
    }
}

/** Helper function to extract the subject from JWT token */
private suspend fun getSubjectFromCall(call: ApplicationCall): String? {
    val principal = call.principal<JWTPrincipal>()
    if (principal == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
        return null
    }

    val subject = principal.payload.getClaim("sub").asString()
    if (subject.isNullOrBlank()) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token: missing user ID"))
        return null
    }
    return subject
}
