package se.moshicon.klerk_todo.http

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.klerkframework.klerk.*

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import se.moshicon.klerk_todo.Ctx
import se.moshicon.klerk_todo.Data
import se.moshicon.klerk_todo.McpServerConfig
import se.moshicon.klerk_todo.users.*

fun Application.configureHttpRouting(klerk: Klerk<Ctx, Data>, mcpServerConfig: McpServerConfig) {
    // Configure JWT authentication
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Todo App"
            verifier(
                JWT.require(Algorithm.HMAC256(JWT_SECRET))
                    .build()
            )
            validate { credential ->
                // Accept any token that passes our simple verification
                JWTPrincipal(credential.payload)
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }

//    suspend fun contextFromCall(call: ApplicationCall): Ctx = call.context(klerk)
//    val lowCodeConfig = LowCodeConfig(
//        basePath = "/admin",
//        contextProvider = ::contextFromCall,
//        showOptionalParameters = { false },
//        cssPath = "https://unpkg.com/almond.css@latest/dist/almond.min.css",
//    )

    routing {
        route("/api") {
            // No authentication for user management because the browser simulates the Identity Provider (IdP).
            // In a real app this URL should be protected, and a real IdP should be used.
            route("/users") {
                apply(registerUsersRoutes(klerk))
            }

            // Protected routes (require authentication)
            authenticate("auth-jwt") {
                route("/todos") {
                    apply(registerTodoRoutes(klerk))
                }

                route("/chat") {
                    apply(registerChatRoutes(klerk, mcpServerConfig))
                }
            }
        }

        route("/serverside") {
            apply(registerServersideRoutes(klerk))
        }

//        // The auto-generated Admin UI
//        val autoAdminUI = LowCodeMain(klerk, lowCodeConfig)
//        apply(autoAdminUI.registerRoutes())


//        route("/mcp") {
//            mcp {
//                Server(
//                    serverInfo = Implementation(
//                        name = "example-sse-server",
//                        version = "1.0.0"
//                    ),
//                    options = ServerOptions(
//                        capabilities = ServerCapabilities(
//                            prompts = ServerCapabilities.Prompts(listChanged = null),
//                            resources = ServerCapabilities.Resources(subscribe = null, listChanged = null)
//                        )
//                    )
//                )
//            }
//        }
    }
//
//    routing {
//        install(SSE)
//
//        apply(configureMcpServer())
//    }


}

/**
 * Creates a Context from a Call.
 * Extracts user information from JWT token if present.
 */
suspend fun ApplicationCall.context(klerk: Klerk<Ctx, Data>): Ctx {
    val principal = this.principal<JWTPrincipal>()

    return if (principal != null) {
        getKlerkContextFromJWT(klerk, principal.payload)
    } else if (request.cookies["user_info"] != null) {
        // Extract username and groups from cookie (not sure, don't use in production)
        val cookieValue = request.cookies["user_info"]!!
        val parts = cookieValue.split(":")

        if (parts.isNotEmpty()) {
            val username = parts[0]
            // Extract groups if present, otherwise empty list
            val groups = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].split(",")
            } else {
                listOf()
            }

            val user = findOrCreateUser(klerk, username)
            return Ctx(GroupModelIdentity(model = user, groups = groups))
        } else {
            // Invalid cookie format, use unauthenticated identity
            Ctx(Unauthenticated)
        }
    } else {
        // Fallback to unauthenticated identity
        Ctx(Unauthenticated)
    }
}

