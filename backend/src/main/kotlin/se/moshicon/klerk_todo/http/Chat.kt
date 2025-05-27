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

    // Get current session or create new one
    get("/session") {
        val userId = getUserIdFromCall(call) ?: return@get

        try {
            val session = ChatSessionManager.getOrCreateSession(userId)
            call.respond(HttpStatusCode.OK, session.toResponse())
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get session: ${e.message}"))
        }
    }

    // Get session history
    get("/history") {
        val userId = getUserIdFromCall(call) ?: return@get

        try {
            val session = ChatSessionManager.getSessionByUserId(userId)
            if (session != null) {
                call.respond(HttpStatusCode.OK, session.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No active session found"))
            }
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
                content = request.content.trim(),
                role = request.role
            )

            val session = ChatSessionManager.addMessage(userId, message)

            call.respond(HttpStatusCode.OK, ChatMessageResponse(
                message = message,
                sessionId = session.sessionId
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to send message: ${e.message}"))
        }
    }

    // Clear session history
    delete("/session") {
        val userId = getUserIdFromCall(call) ?: return@delete

        try {
            val session = ChatSessionManager.clearSession(userId)
            if (session != null) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Session cleared successfully"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No active session found"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to clear session: ${e.message}"))
        }
    }

    // Get session statistics (for debugging/monitoring)
    get("/stats") {
        try {
            val stats = ChatSessionManager.getSessionStats()
            call.respond(HttpStatusCode.OK, stats)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get stats: ${e.message}"))
        }
    }
}

/**
 * Helper function to extract user ID from JWT token
 */
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
