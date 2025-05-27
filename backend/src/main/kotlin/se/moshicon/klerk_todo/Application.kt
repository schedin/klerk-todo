package se.moshicon.klerk_todo

import dev.klerkframework.klerk.AuthenticationIdentity
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.SystemIdentity
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import se.moshicon.klerk_mcp.createMcpServer
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.mcp

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import se.moshicon.klerk_todo.http.configureHttpRouting
import se.moshicon.klerk_todo.http.findOrCreateUser
import se.moshicon.klerk_todo.notes.*
import se.moshicon.klerk_todo.users.*
import kotlin.time.measureTime

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

    suspend fun contextProvider(command: Command<*, *>?): Ctx {
        val user = findOrCreateUser(klerk, "Alice")
        return Ctx(GroupModelIdentity(model = user, groups = listOf("admins", "users")))
    }

    val mcpServer = createMcpServer(klerk, ::contextProvider, "TODO application", "1.0.0")
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
        configureHttpRouting(klerk)

//        install(SSE)
//        routing {
//            route("myRoute") {
//                mcp {
//                    getMcpServer()
//                }
//            }
//        }
        //Due do a bug in kotlin-sdk for MCP (https://github.com/modelcontextprotocol/kotlin-sdk/issues/94) it is
        // currently not possible to control the URL for the MCP server.
        mcp {
            mcpServer
        }


    }.start(wait = true)
}




