package se.moshicon.klerk_todo.chat

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam

/**
 * Enum to represent who sent the message
 */
@Serializable
enum class MessageSender {
    USER,
    ASSISTANT
}

/**
 * Represents a single message in a chat conversation for the browser
 */
@Serializable
data class BrowserChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val sender: MessageSender,
    val timestamp: Long = Instant.now().epochSecond
)

/**
 * Represents an internal message for LLM communication
 */
data class InternalChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val messageParam: ChatCompletionMessageParam,
    val timestamp: Long = Instant.now().epochSecond
)

/**
 * Legacy ChatMessage for backward compatibility during transition
 */
@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = Instant.now().epochSecond
)

/**
 * Represents a chat session for a specific user
 */
data class ChatSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userName: String,
    val userId: String,
    val internalMessages: MutableList<InternalChatMessage> = mutableListOf(),
    val createdAt: Instant = Instant.now(),
    var lastAccessedAt: Instant = Instant.now()
) {
    /**
     * Adds an internal message to the session and updates the last accessed time
     */
    fun addInternalMessage(message: InternalChatMessage) {
        internalMessages.add(message)
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
     * Gets the conversation history as a list of browser messages by converting internal messages
     */
    fun getHistory(): List<BrowserChatMessage> {
        return internalMessages.mapNotNull { internalMsg ->
            val messageParam = internalMsg.messageParam
            when {
                messageParam.isUser() -> BrowserChatMessage(
                    id = internalMsg.id,
                    content = messageParam.asUser().content().text().get(),
                    sender = MessageSender.USER,
                    timestamp = internalMsg.timestamp
                )
                messageParam.isAssistant() -> {
                    // Only include assistant messages that have content (skip tool call messages)
                    val content = messageParam.asAssistant().content()
                    if (content.isPresent) {
                        val contentStr = content.get().text().get()
                        if (contentStr.isNotBlank()) {
                            BrowserChatMessage(
                                id = internalMsg.id,
                                content = contentStr,
                                sender = MessageSender.ASSISTANT,
                                timestamp = internalMsg.timestamp
                            )
                        } else null
                    } else null
                }
                // Skip system messages and tool messages - they're not shown to the user
                else -> null
            }
        }
    }

    /**
     * Gets the internal conversation history for LLM communication
     */
    fun getInternalHistory(): List<ChatCompletionMessageParam> = internalMessages.map { it.messageParam }

    /**
     * Clears all messages from the session
     */
    fun clearHistory() {
        internalMessages.clear()
        lastAccessedAt = Instant.now()
    }

    // Legacy methods for backward compatibility during transition
    @Deprecated("Use addInternalMessage instead")
    fun addMessage(message: ChatMessage) {
        val userInternalMessage = InternalChatMessage(
            id = message.id,
            messageParam = ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                    .content(message.content)
                    .build()
            ),
            timestamp = message.timestamp
        )
        addInternalMessage(userInternalMessage)
    }
}

/**
 * Request/Response DTOs for the chat API
 */
@Serializable
data class ChatMessageRequest(
    val content: String
)

@Serializable
data class ChatMessageResponse(
    val message: BrowserChatMessage
)

@Serializable
data class ChatHistoryResponse(
    val messages: List<BrowserChatMessage>
)
