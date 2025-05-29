package se.moshicon.klerk_todo


import dev.klerkframework.klerk.Klerk
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.http.*

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.GlobalScope
import org.slf4j.LoggerFactory
import se.moshicon.klerk_todo.http.configureHttpRouting
import se.moshicon.klerk_todo.http.findOrCreateUser
import se.moshicon.klerk_todo.users.*
import se.moshicon.klerk_mcp.createMcpServer
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import se.moshicon.klerk_todo.chat.ChatSessionManager
import se.moshicon.klerk_todo.chat.ChatEngine

private val logger = LoggerFactory.getLogger("se.moshicon.klerk_todo.Application")

fun main() {
    val klerk = Klerk.create(createConfig())
    runBlocking {
        klerk.meta.start()
        if (klerk.meta.modelsCount == 0) {
            createExampleData(klerk)
        }
    }
//    performanceInsertTest(klerk)

    startMcpServer(klerk)

    // Create and initialize ChatEngine with MCP connection
    val chatEngine = ChatEngine(
        Url(McpClientConfig.mcpServerUrl),
        Url(McpClientConfig.llmServerUrl),
        McpClientConfig.llmApiKey
    )

    // Initialize MCP server connection
    runBlocking {
        try {
            logger.info("Initializing MCP server connection...")
            chatEngine.initMcpServerConnection()
            logger.info("MCP server connection initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize MCP server connection: ${e.message}", e)
            // Continue startup even if MCP connection fails
        }
    }

    ChatSessionManager.initialize(klerk, GlobalScope, chatEngine)
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down chat session manager...")
        ChatSessionManager.shutdown()
    })

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        // Configure CORS to allow frontend requests
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            allowCredentials = true
            anyHost() // For development only - restrict in production
        }

        // Configure JSON serialization
        install(ContentNegotiation) {
            json()
        }
        configureHttpRouting(klerk, McpServerConfig)


    }.start(wait = true)
}

fun startMcpServer(klerk: Klerk<Ctx, Data>) {
    logger.info("Starting MCP server on port ${McpServerConfig.PORT}...")

    suspend fun contextProvider(command: dev.klerkframework.klerk.command.Command<*, *>?): Ctx {
        val user = findOrCreateUser(klerk, "Alice")
        return Ctx(GroupModelIdentity(model = user, groups = listOf("admins", "users")))
    }

    val mcpServer = createMcpServer(klerk, ::contextProvider, McpServerConfig.SERVER_NAME, McpServerConfig.SERVER_VERSION)

    // Start MCP server in a separate thread so it doesn't block the main server
    Thread {
        embeddedServer(Netty, port = McpServerConfig.PORT, host = McpServerConfig.HOST) {
            mcp {
                mcpServer
            }
        }.start(wait = true)
    }.start()

    logger.info("MCP server started at ${McpServerConfig.getServerUrl()}")
}




