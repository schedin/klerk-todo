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
import kotlinx.serialization.Serializable
import se.moshicon.klerk_todo.Ctx
import se.moshicon.klerk_todo.Data
import se.moshicon.klerk_todo.McpServerConfig
import se.moshicon.klerk_todo.chat.*

fun registerChatRoutes(klerk: Klerk<Ctx, Data>, mcpServerConfig: McpServerConfig): Route.() -> Unit = {

    // Get chat history
    get("/history") {
        val userId = getUserIdFromCall(call) ?: return@get

        try {
            val session = ChatSessionManager.getSessionByUserId(userId)
            val messages = session?.getHistory() ?: emptyList()
            call.respond(HttpStatusCode.OK, ChatHistoryResponse(messages))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get history: ${e.message}"))
        }
    }

    // Send a message
    post("/message") {
        val userId = getUserIdFromCall(call) ?: return@post

        try {
            val request = call.receive<ChatMessageRequest>()

            // Validate message content
            if (request.content.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Message content cannot be empty"))
                return@post
            }

            val message = ChatMessage(
                content = request.content.trim()
            )
            ChatSessionManager.addMessage(userId, message)

            val responseMessage = ChatMessage(
                content = "You said ${request.content}."
            )
            ChatSessionManager.addMessage(userId, responseMessage)

            call.respond(HttpStatusCode.OK, ChatMessageResponse(responseMessage))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to send message: ${e.message}"))
        }
    }

    // Clear chat history
    delete("/history") {
        val userId = getUserIdFromCall(call) ?: return@delete

        try {
            ChatSessionManager.clearSession(userId)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Chat history cleared successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to clear history: ${e.message}"))
        }
    }

//    // Get session statistics (for debugging/monitoring)
//    get("/stats") {
//        try {
//            val stats = ChatSessionManager.getSessionStats()
//            call.respond(HttpStatusCode.OK, stats)
//        } catch (e: Exception) {
//            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get stats: ${e.message}"))
//        }
//    }
}

/**
 * Helper function to extract user ID from JWT token
 */
private suspend fun getUserIdFromCall(call: ApplicationCall): String? {
    return "Alice"

//    val principal = call.principal<JWTPrincipal>()
//    if (principal == null) {
//        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
//        return null
//    }
//
//    val userId = principal.payload.getClaim("sub").asString()
//    if (userId.isNullOrBlank()) {
//        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token: missing user ID"))
//        return null
//    }
//    return userId
}
