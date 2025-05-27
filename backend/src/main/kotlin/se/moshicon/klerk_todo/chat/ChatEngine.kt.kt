package se.moshicon.klerk_todo.chat

import io.ktor.http.*

class ChatEngine(
    private val mcpServerUrl: Url,
    private val llmServerUrl: Url
) {

    fun handleChatMessage(chatSession: ChatSession, message: ChatMessage): ChatMessage {
        chatSession.addMessage(message)
        val responseMessage = ChatMessage(
            content = "You said ${message.content}."
        )
        chatSession.addMessage(responseMessage)
        return responseMessage
    }

}