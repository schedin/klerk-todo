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

suspend fun createExampleData(klerk: Klerk<Ctx, Data>) {
    createInitialUsers(klerk)
    createInitialTodo(klerk)
}

suspend fun createInitialUsers(klerk: Klerk<Ctx, Data>) {
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

suspend fun createInitialTodo(klerk: Klerk<Ctx, Data>) {
    suspend fun createTodo(username: String, title: String, desc: String, priority: Int) {
        val user = klerk.read(Ctx(SystemIdentity)) {
            getFirstWhere(data.users.all) {
                it.props.name.value == username
            }
        }

        val context = Ctx(GroupModelIdentity(model = user, groups = listOf("users")))
        val command = Command(
            event = CreateTodo,
            model = null,
            params = CreateTodoParams(
                title = TodoTitle(title),
                description = TodoDescription(desc),
                username = UserName(username),
                priority = TodoPriority(priority),
            ),
        )
        klerk.handle(command, context, ProcessingOptions(CommandToken.simple())).orThrow()
    }

    createTodo("Alice", "Perform code review", "Send code review comments to Bob", 4)
    createTodo("Alice", "Deploy application", "Deply the application to production in Kubernetes", 2)
    createTodo("Alice", "Send invoice", "Send invoice to customer to invoice@example.com", 8)
    createTodo("Bob", "Buy milk", "Go to the store and buy milk", 6)
}