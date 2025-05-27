package se.moshicon.klerk_todo

import dev.klerkframework.klerk.AuthenticationIdentity
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.SystemIdentity
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import se.moshicon.klerk_todo.notes.*
import se.moshicon.klerk_todo.users.CreateUser
import se.moshicon.klerk_todo.users.CreateUserParams
import se.moshicon.klerk_todo.users.GroupModelIdentity
import se.moshicon.klerk_todo.users.UserName

suspend fun createInitialUsers(klerk: Klerk<Ctx, Data>) {
    val users = klerk.read(Ctx(AuthenticationIdentity)) {
        list(data.users.all)
    }
    if (users.isEmpty()) {
        suspend fun createUser(username: String) {
            val command = Command(
                event = CreateUser,
                model = null,
                params = CreateUserParams(UserName(username)),
            )
            klerk.handle(command, Ctx(AuthenticationIdentity), ProcessingOptions(CommandToken.simple()))
        }
        createUser("Alice")
        createUser("Bob")
        createUser("Charlie")
    }
}

suspend fun createInitialTodo(klerk: Klerk<Ctx, Data>) {
    val context = Ctx(SystemIdentity)
    val numberOfTodos = klerk.read(context) {
        list(data.todos.all).size
    }
    if (numberOfTodos != 0) {
        return
    }

    val user = (context.actor as? GroupModelIdentity)?.model?.props
        ?: throw IllegalStateException("User not found in context")

    val command = Command(
        event = CreateTodo,
        model = null,
        params = CreateTodoParams(
            title = TodoTitle("Buy milk"),
            description = TodoDescription("Go to the store and buy milk"),
            username = user.name,
            priority = TodoPriority(6),
        ),
    )

    klerk.handle(command, context, ProcessingOptions(CommandToken.simple()))

}