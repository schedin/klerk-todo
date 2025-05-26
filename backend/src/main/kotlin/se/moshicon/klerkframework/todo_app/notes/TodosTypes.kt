package se.moshicon.klerkframework.todo_app.notes

import dev.klerkframework.klerk.InvalidParametersProblem
import dev.klerkframework.klerk.ModelID
import dev.klerkframework.klerk.datatypes.IntContainer
import dev.klerkframework.klerk.datatypes.StringContainer
import se.moshicon.klerkframework.todo_app.users.User

data class Todo(
    val title: TodoTitle,
    val description: TodoDescription,
    val priority: TodoPriority = TodoPriority(0),
    val userID: ModelID<User>,
)

class TodoPriority(value: Int) : IntContainer(value) {
    override val min = 0
    override val max = 10
    override val validators = setOf<() -> InvalidParametersProblem?>( ::checkMe )

    fun checkMe(): InvalidParametersProblem? {
        if (this.value % 2 != 0) {
            return InvalidParametersProblem(message = "Only even priorities are allowed. (what!?)")
        }
        return null
    }
}

class TodoTitle(value: String) : StringContainer(value) {
    override val minLength = 1
    override val maxLength = 100
    override val maxLines = 1
}

class TodoDescription(value: String) : StringContainer(value) {
    override val minLength = 0
    override val maxLength = 100000
    override val maxLines = Int.MAX_VALUE
}
