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
import se.moshicon.klerk_todo.Ctx
import se.moshicon.klerk_todo.Data
import se.moshicon.klerk_todo.McpServerConfig
import se.moshicon.klerk_todo.chat.*

fun registerChatRoutes(klerk: Klerk<Ctx, Data>, mcpServerConfig: McpServerConfig): Route.() -> Unit = {
    get("/history") { getChatHistory(call) }
    post("/message") { addNewChatMessage(call) }
    delete("/history") { clearChatHistory(call) }
}

private suspend fun getChatHistory(call: ApplicationCall) {
    val userId = getUserIdFromCall(call) ?: return
    try {
        val session = ChatSessionManager.getSessionByUserId(userId)
        val messages = session?.getHistory() ?: emptyList()
        call.respond(HttpStatusCode.OK, ChatHistoryResponse(messages))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get history: ${e.message}"))
    }
}

private suspend fun addNewChatMessage(call: ApplicationCall) {
    val userId = getUserIdFromCall(call) ?: return
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

        val chatSession = ChatSessionManager.getOrCreateSession(userId)
        val responseMessage = handleChatMessage(chatSession, chatMessage)
        call.respond(HttpStatusCode.OK, ChatMessageResponse(responseMessage))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to send message: ${e.message}"))
    }
}

private fun handleChatMessage(chatSession: ChatSession, message: ChatMessage): ChatMessage {
    chatSession.addMessage(message)
    val responseMessage = ChatMessage(
        content = "You said ${message.content}."
    )
    chatSession.addMessage(responseMessage)
    return responseMessage
}

private suspend fun clearChatHistory(call: ApplicationCall) {
    val userId = getUserIdFromCall(call) ?: return

    try {
        val chatSession = ChatSessionManager.getSessionByUserId(userId)
        chatSession?.clearHistory()
        call.respond(HttpStatusCode.OK, mapOf("message" to "Chat history cleared successfully"))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to clear history: ${e.message}"))
    }
}

/** Helper function to extract user ID from JWT token */
private suspend fun getUserIdFromCall(call: ApplicationCall): String? {
    val principal = call.principal<JWTPrincipal>()
    if (principal == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
        return null
    }

    val userId = principal.payload.getClaim("sub").asString()
    if (userId.isNullOrBlank()) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token: missing user ID"))
        return null
    }
    return userId
}
