package se.moshicon.klerk_todo.chat

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

/**
 * Represents a single message in a chat conversation
 */
@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = Instant.now().epochSecond
)

/**
 * Enum representing the role of a message sender
 */
@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Represents a chat session for a specific user
 */
data class ChatSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: Instant = Instant.now(),
    var lastAccessedAt: Instant = Instant.now()
) {
    /**
     * Adds a message to the session and updates the last accessed time
     */
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        lastAccessedAt = Instant.now()
    }

    /**
     * Updates the last accessed time
     */
    fun touch() {
        lastAccessedAt = Instant.now()
    }

    /**
     * Checks if the session has expired based on the given timeout duration
     */
    fun isExpired(timeoutMinutes: Long): Boolean {
        val timeoutInstant = Instant.now().minusSeconds(timeoutMinutes * 60)
        return lastAccessedAt.isBefore(timeoutInstant)
    }

    /**
     * Gets the conversation history as a list of messages
     */
    fun getHistory(): List<ChatMessage> = messages.toList()

    /**
     * Clears all messages from the session
     */
    fun clearHistory() {
        messages.clear()
        lastAccessedAt = Instant.now()
    }
}

/**
 * Request/Response DTOs for the chat API
 */
@Serializable
data class ChatMessageRequest(
    val content: String,
    val role: MessageRole = MessageRole.USER
)

@Serializable
data class ChatMessageResponse(
    val message: ChatMessage,
    val sessionId: String
)

@Serializable
data class ChatSessionResponse(
    val sessionId: String,
    val messages: List<ChatMessage>,
    val createdAt: Long,
    val lastAccessedAt: Long
)

/**
 * Extension function to convert ChatSession to ChatSessionResponse
 */
fun ChatSession.toResponse(): ChatSessionResponse = ChatSessionResponse(
    sessionId = sessionId,
    messages = messages,
    createdAt = createdAt.epochSecond,
    lastAccessedAt = lastAccessedAt.epochSecond
)
