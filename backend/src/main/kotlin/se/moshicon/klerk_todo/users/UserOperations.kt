package se.moshicon.klerk_todo.users

import dev.klerkframework.klerk.AuthenticationIdentity
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import se.moshicon.klerk_todo.Ctx
import se.moshicon.klerk_todo.Data

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

//suspend fun deleteUser(klerk: Klerk<Ctx, Data>, username: String) {
//    val userToDelete = klerk.read(Ctx(AuthenticationIdentity)) {
//        firstOrNull(data.users.all) { it.props.name.value == username }
//    }
//    if (userToDelete != null) {
//        val command = Command(
//            event = DeleteUser,
//            model = userToDelete.id,
//            params = null,
//        )
//        klerk.handle(command, Ctx(Unauthenticated), ProcessingOptions(CommandToken.simple()))
//    }
//}