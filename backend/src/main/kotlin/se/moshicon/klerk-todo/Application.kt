package se.moshicon.klerkframework.todo_app

import dev.klerkframework.klerk.AuthenticationIdentity
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import dev.klerkframework.mcp.createMcpServer
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
import se.moshicon.klerkframework.todo_app.http.configureHttpRouting
import se.moshicon.klerkframework.todo_app.http.findOrCreateUser
import se.moshicon.klerkframework.todo_app.notes.*
import se.moshicon.klerkframework.todo_app.users.*
import kotlin.time.measureTime

private val logger = LoggerFactory.getLogger(Application::class.java.name)

fun main() {
    val klerk = Klerk.create(createConfig())
    runBlocking {
        klerk.meta.start()
        createInitialUsers(klerk)
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

fun performanceInsertTest(klerk: Klerk<Ctx, Data>) {
    logger.info("Starting performance insert test...")

    // Simple performance test
//    runBlocking {
//        val aliceUser = klerk.read(Ctx(AuthenticationIdentity)) {
//            getFirstWhere(data.users.all) { it.props.name.value == "Alice" }
//        }
//
//        for (i in 0 until 10) {
//            val context = Ctx(GroupModelIdentity(model = aliceUser, groups = listOf("admins", "users")))
//            val command = Command(
//                event = CreateTodo,
//                model = null,
//                params = CreateTodoParams(
//                    title = TodoTitle("test Title"),
//                    description = TodoDescription("test desc"),
//                    username = UserName("Alice"),
//                    priority = TodoPriority(4),
//                ),
//            )
//            val result = klerk.handle(command, context, ProcessingOptions(CommandToken.simple()))
//        }
//    }

    runBlocking {
        val aliceUser = klerk.read(Ctx(AuthenticationIdentity)) {
            getFirstWhere(data.users.all) { it.props.name.value == "Alice" }
        }
        val totalTime = measureTime {
            for (j in 0 until 100) {
                val seconds = measureTime {
                    for (i in 0 until 10000) {
                        val context = Ctx(GroupModelIdentity(model = aliceUser, groups = listOf("admins", "users")))
                        val command = Command(
                            event = CreateTodo,
                            model = null,
                            params = CreateTodoParams(
                                title = TodoTitle("test Title"),
                                description = TodoDescription("test desc"),
                                username = UserName("Alice"),
                                priority = TodoPriority(4),
                            ),
                        )
                        val result = klerk.handle(command, context, ProcessingOptions(CommandToken.simple()))
//            println(result)
//            delay(1)
                    }
                }.inWholeSeconds
                println("10000 commands took $seconds seconds")
            }
        }.inWholeSeconds
        println("100 iterations of 10000 commands took $totalTime seconds")

    }
}
