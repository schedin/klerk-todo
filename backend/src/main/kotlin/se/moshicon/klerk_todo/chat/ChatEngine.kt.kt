package se.moshicon.klerk_todo.chat

class ChatEngine {


    fun handleChatMessage(chatSession: ChatSession, message: ChatMessage): ChatMessage {
        chatSession.addMessage(message)
        val responseMessage = ChatMessage(
            content = "You said ${message.content}."
        )
        chatSession.addMessage(responseMessage)
        return responseMessage
    }

}