package se.moshicon.klerk_todo.http

import com.auth0.jwt.interfaces.Payload
import dev.klerkframework.klerk.AuthenticationIdentity
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.Model
import dev.klerkframework.klerk.Unauthenticated
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import se.moshicon.klerk_todo.Ctx
import se.moshicon.klerk_todo.Data
import se.moshicon.klerk_todo.users.*

// JWT configuration constants
// Note: In this demo, we're using a simplified JWT implementation signature verification process that probably
// shouldn't be used in production. In a real app you should use a public/private key way of verifying the token.
const val JWT_SECRET = "your-secret-key"

suspend fun getKlerkContextFromJWT(klerk: Klerk<Ctx, Data>, decodedJWT: Payload): Ctx {
    val username = decodedJWT.getClaim("sub").asString() ?: return Ctx(Unauthenticated)

    val user = findOrCreateUser(klerk, username)
    val groups = decodedJWT.getClaim("groups").asList(String::class.java) ?: listOf()
    return Ctx(GroupModelIdentity(model = user, groups = groups))
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
