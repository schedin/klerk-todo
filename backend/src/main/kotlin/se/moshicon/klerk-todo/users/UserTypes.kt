package se.moshicon.klerkframework.todo_app.users

import dev.klerkframework.klerk.datatypes.StringContainer

data class User(
    val name: UserName,
)

class UserName(value: String) : StringContainer(value) {
    override val minLength = 3
    override val maxLength = 50
    override val maxLines = 1
}

class CreateUserParams(val name: UserName)

