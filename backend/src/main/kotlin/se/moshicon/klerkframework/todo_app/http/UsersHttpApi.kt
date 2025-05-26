package se.moshicon.klerkframework.todo_app.http

import dev.klerkframework.klerk.AuthenticationIdentity
import dev.klerkframework.klerk.CommandResult.Failure
import dev.klerkframework.klerk.CommandResult.Success
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.Model
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import io.ktor.http.*

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import se.moshicon.klerkframework.todo_app.Ctx
import se.moshicon.klerkframework.todo_app.Data
import se.moshicon.klerkframework.todo_app.users.DeleteAllUserTodos
import se.moshicon.klerkframework.todo_app.users.DeleteUser
import se.moshicon.klerkframework.todo_app.users.User

fun registerUsersRoutes(klerk: Klerk<Ctx, Data>): Route.() -> Unit = {
    get("/{...}") { getUsers(call, klerk) }
    delete("/{username}") { deleteUser(call, klerk) }
}


@Serializable
data class UserResponse(
    val username: String,
)

fun toUserResponse(user: Model<User>) = UserResponse(
    username = user.props.name.value,
)

suspend fun getUsers(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    val context = call.context(klerk)
    val users = klerk.read(context) {
        list(data.users.all).map { user -> toUserResponse(user) }
    }
    call.respond(users)
}


suspend fun deleteUser(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    var context = Ctx(AuthenticationIdentity)
    val username = call.parameters["username"]
    val userToDelete = klerk.read(context) {
        firstOrNull(data.users.all) {
            it.props.name.value == username
        }
    }

    if (userToDelete == null) {
        call.respond(HttpStatusCode.NotFound, "User $username not found")
        return
    }

    // The current implementation of Klerk state machine handling does allow deleting of todos and the user in the
    // same transaction. To workaround this, we first delete all todos for the user.
    Command(
        event = DeleteAllUserTodos,
        model = userToDelete.id,
        params = null,
    ).run {
        when(val result = klerk.handle(this, context, ProcessingOptions(CommandToken.simple()))) {
            is Failure -> {
                call.respond(HttpStatusCode.BadRequest, result.problem.toString())
                return
            }
            is Success -> { }
        }
    }

    context = Ctx(AuthenticationIdentity)

    val command = Command(
        event = DeleteUser,
        model = userToDelete.id,
        params = null,
    )
    when(val result = klerk.handle(command, context, ProcessingOptions(CommandToken.simple()))) {
        is Failure -> {
            call.respond(HttpStatusCode.BadRequest, result.problem.toString())
        }
        is Success -> {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
