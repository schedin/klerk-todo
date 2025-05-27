package se.moshicon.klerk_todo.chat

import kotlinx.coroutines.*

/**
 * Simple test class for ChatSession functionality
 * Run this manually to verify the chat session management works correctly
 */
object ChatSessionTest {

    fun runAllTests() {
        println("Starting ChatSession tests...")

        // Initialize with a test scope
        ChatSessionManager.initialize(CoroutineScope(Dispatchers.Default))

        try {
            testCreateNewSession()
            testSameSessionForSameUser()
            testAddMessage()
            testClearSession()
            testSessionStats()
            testRemoveUserSessions()
            testSessionExpiration()

            println("All tests passed!")
        } catch (e: Exception) {
            println("Test failed: ${e.message}")
            e.printStackTrace()
        } finally {
            ChatSessionManager.shutdown()
        }
    }

    private fun testCreateNewSession() {
        println("Testing: should create new session for user")
        val userId = "testUser1"
        val session = ChatSessionManager.getOrCreateSession(userId)

        assert(session.userId == userId) { "User ID should match" }
        assert(session.messages.isEmpty()) { "Messages should be empty initially" }
        println("✓ Create new session test passed")
    }

    private fun testSameSessionForSameUser() {
        println("Testing: should return same session for same user")
        val userId = "testUser1"
        val session1 = ChatSessionManager.getOrCreateSession(userId)
        val session2 = ChatSessionManager.getOrCreateSession(userId)

        assert(session1.sessionId == session2.sessionId) { "Session IDs should match" }
        println("✓ Same session test passed")
    }

    private fun testAddMessage() {
        println("Testing: should add message to session")
        val userId = "testUser2"
        val message = ChatMessage(content = "Hello", role = MessageRole.USER)

        val session = ChatSessionManager.addMessage(userId, message)

        assert(session.messages.size == 1) { "Should have 1 message" }
        assert(session.messages[0].content == "Hello") { "Message content should match" }
        assert(session.messages[0].role == MessageRole.USER) { "Message role should match" }
        println("✓ Add message test passed")
    }

    private fun testClearSession() {
        println("Testing: should clear session history")
        val userId = "testUser3"
        val message = ChatMessage(content = "Hello", role = MessageRole.USER)

        // Add a message
        ChatSessionManager.addMessage(userId, message)
        var session = ChatSessionManager.getSessionByUserId(userId)
        assert(session?.messages?.size == 1) { "Should have 1 message before clear" }

        // Clear the session
        ChatSessionManager.clearSession(userId)
        session = ChatSessionManager.getSessionByUserId(userId)
        assert(session?.messages?.size == 0) { "Should have 0 messages after clear" }
        println("✓ Clear session test passed")
    }

    private fun testSessionStats() {
        println("Testing: should get session statistics")
        val userId1 = "testUser4"
        val userId2 = "testUser5"

        // Create sessions and add messages
        ChatSessionManager.addMessage(userId1, ChatMessage(content = "Hello", role = MessageRole.USER))
        ChatSessionManager.addMessage(userId2, ChatMessage(content = "Hi", role = MessageRole.USER))
        ChatSessionManager.addMessage(userId1, ChatMessage(content = "How are you?", role = MessageRole.USER))

        val stats = ChatSessionManager.getSessionStats()

        assert(stats.totalSessions >= 2) { "Should have at least 2 sessions" }
        assert(stats.activeSessions >= 2) { "Should have at least 2 active sessions" }
        assert(stats.totalMessages >= 3) { "Should have at least 3 messages" }
        println("✓ Session stats test passed")
    }

    private fun testRemoveUserSessions() {
        println("Testing: should remove user sessions")
        val userId = "testUser6"
        val message = ChatMessage(content = "Hello", role = MessageRole.USER)

        // Create session
        ChatSessionManager.addMessage(userId, message)
        assert(ChatSessionManager.getSessionByUserId(userId) != null) { "Session should exist" }

        // Remove session
        ChatSessionManager.removeUserSessions(userId)
        assert(ChatSessionManager.getSessionByUserId(userId) == null) { "Session should be removed" }
        println("✓ Remove user sessions test passed")
    }

    private fun testSessionExpiration() {
        println("Testing: should handle session expiration")
        val session = ChatSession(userId = "testUser")

        // Session should not be expired initially
        assert(!session.isExpired(30)) { "Session should not be expired initially" }

        // Simulate old session by setting lastAccessedAt to past
        val oldSession = session.copy(
            lastAccessedAt = java.time.Instant.now().minusSeconds(31 * 60) // 31 minutes ago
        )

        // Session should be expired
        assert(oldSession.isExpired(30)) { "Old session should be expired" }
        println("✓ Session expiration test passed")
    }
}
