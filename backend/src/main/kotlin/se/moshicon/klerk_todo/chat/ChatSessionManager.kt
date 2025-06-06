package se.moshicon.klerk_todo.chat

import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.SystemIdentity
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import se.moshicon.klerk_todo.Ctx
import se.moshicon.klerk_todo.Data
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * Singleton manager for handling chat sessions in memory
 */
object ChatSessionManager {

    private val logger = LoggerFactory.getLogger(ChatSessionManager::class.java)
    private val sessions = ConcurrentHashMap<String, ChatSession>()
    private val userSessions = ConcurrentHashMap<String, String>() // userName -> sessionId
    lateinit var chatEngine: ChatEngine
        private set
    private lateinit var klerk: Klerk<Ctx, Data>

    // Configuration
    private const val SESSION_TIMEOUT_MINUTES = 30L
    private const val CLEANUP_INTERVAL_MINUTES = 5L

    private var cleanupJob: Job? = null

    /**
     * Initializes the session manager and starts the cleanup coroutine
     */
    fun initialize(klerk: Klerk<Ctx, Data>, scope: CoroutineScope, chatEngine: ChatEngine) {
        this.klerk = klerk
        this.chatEngine = chatEngine
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(CLEANUP_INTERVAL_MINUTES.minutes)
                cleanupExpiredSessions()
            }
        }
        logger.info("Chat session manager initialized")
    }

    /**
     * Stops the session manager and cancels cleanup
     */
    fun shutdown() {
        cleanupJob?.cancel()
        sessions.clear()
        userSessions.clear()
    }

    /**
     * Gets or creates a session for the given user
     */
    suspend fun getOrCreateSession(userName: String): ChatSession {
        // Check if user already has an active session
        val existingSessionId = userSessions[userName]
        if (existingSessionId != null) {
            val existingSession = sessions[existingSessionId]
            if (existingSession != null && !existingSession.isExpired(SESSION_TIMEOUT_MINUTES)) {
                existingSession.touch()
                return existingSession
            } else {
                // Session expired, remove it
                sessions.remove(existingSessionId)
                userSessions.remove(userName)
            }
        }

        val userId = klerk.read(Ctx(SystemIdentity)) {
            getFirstWhere(data.users.all) {
                it.props.name.value == userName
            }.id.toString()
        }
        // Create new session
        val newSession = ChatSession(userName = userName, userId = userId)
        sessions[newSession.sessionId] = newSession
        userSessions[userName] = newSession.sessionId

        return newSession
    }

    /**
     * Gets a session by session ID
     */
    private fun getSession(sessionId: String): ChatSession? {
        val session = sessions[sessionId]
        if (session != null && !session.isExpired(SESSION_TIMEOUT_MINUTES)) {
            session.touch()
            return session
        } else if (session != null) {
            // Session expired, remove it
            removeSession(sessionId)
        }
        return null
    }

    /**
     * Gets a session by user ID
     */
    fun getSessionByUserName(userName: String): ChatSession? {
        val sessionId = userSessions[userName] ?: return null
        return getSession(sessionId)
    }

    /**
     * Removes a specific session
     */
    private fun removeSession(sessionId: String) {
        val session = sessions.remove(sessionId)
        if (session != null) {
            userSessions.remove(session.userName)
        }
    }

    /**
     * Gets statistics about current sessions
     */
    fun getSessionStats(): SessionStats {
        val now = Instant.now()
        val activeSessions = sessions.values.count { !it.isExpired(SESSION_TIMEOUT_MINUTES) }
        val totalMessages = sessions.values.sumOf { it.getHistory().size }

        return SessionStats(
            totalSessions = sessions.size,
            activeSessions = activeSessions,
            totalMessages = totalMessages,
            oldestSessionAge = sessions.values.minOfOrNull {
                java.time.Duration.between(it.createdAt, now).toMinutes()
            } ?: 0L
        )
    }

    /**
     * Cleans up expired sessions
     */
    private fun cleanupExpiredSessions() {
        val expiredSessions = sessions.values.filter { it.isExpired(SESSION_TIMEOUT_MINUTES) }

        expiredSessions.forEach { session ->
            sessions.remove(session.sessionId)
            userSessions.remove(session.userName)
        }

        if (expiredSessions.isNotEmpty()) {
            println("Cleaned up ${expiredSessions.size} expired chat sessions")
        }
    }
}

/**
 * Data class for session statistics
 */
data class SessionStats(
    val totalSessions: Int,
    val activeSessions: Int,
    val totalMessages: Int,
    val oldestSessionAge: Long
)
