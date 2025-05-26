package se.moshicon.klerkframework.todo_app.http

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import se.moshicon.klerkframework.todo_app.Ctx
import se.moshicon.klerkframework.todo_app.Data
import se.moshicon.klerkframework.todo_app.users.*

// JWT configuration constants
// Note: In this demo, we're using a simplified JWT implementation without real verification
// In a real app, you would use proper JWT verification with a secure secret key
private const val JWT_SECRET = "your-secret-key"
private const val JWT_ISSUER = "todo-app"
private const val JWT_AUDIENCE = "todo-app-users"

fun Application.configureHttpRouting(klerk: Klerk<Ctx, Data>) {
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
        try {
            // Extract user information from JWT claims
            val username = principal.payload.getClaim("sub").asString()

            // Extract groups from JWT claims (if present)
            val groups = try {
                principal.payload.getClaim("groups").asList(String::class.java) ?: listOf()
            } catch (e: Exception) {
                listOf<String>()
            }
            val user = findOrCreateUser(klerk, username)
            Ctx(GroupModelIdentity(model = user, groups = groups))
        } catch (e: Exception) {
            // Fallback to system identity if JWT parsing fails
            println("Error parsing JWT: ${e.message}")
            Ctx(Unauthenticated)
        }
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

suspend fun findOrCreateUser(klerk: Klerk<Ctx, Data>, username: String): Model<User> {
    val authContext = Ctx(AuthenticationIdentity)

    // Try to find an existing user and return it if found
    return klerk.read(authContext) {
        firstOrNull(data.users.all) { it.props.name.value == username }
    } ?: run {
        // User not found, create a new one, trusting the JWT issuer for what users should exist
        val command = Command(
            event = CreateUser,
            model = null,
            params = CreateUserParams(UserName(username)),
        )
        klerk.handle(command, authContext, ProcessingOptions(CommandToken.simple()))

        // Return the newly created user
        klerk.read(authContext) {
            getFirstWhere(data.users.all) { it.props.name.value == username }
        }
    }
}
