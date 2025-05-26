package se.moshicon.klerkframework.todo_app.notes

import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.actions.Job
import dev.klerkframework.klerk.actions.JobContext
import dev.klerkframework.klerk.actions.JobId
import dev.klerkframework.klerk.actions.JobResult
import dev.klerkframework.klerk.statemachine.stateMachine
import kotlinx.datetime.Instant
import se.moshicon.klerkframework.todo_app.Ctx
import se.moshicon.klerkframework.todo_app.Data
import se.moshicon.klerkframework.todo_app.notes.TodoStates.*
import se.moshicon.klerkframework.todo_app.users.GroupModelIdentity
import se.moshicon.klerkframework.todo_app.users.User
import se.moshicon.klerkframework.todo_app.users.UserName
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds


enum class TodoStates {
    Created,
    Completed,
    Trashed,
}

val todoStateMachine = stateMachine {
    event(CreateTodo) {
        validateWithParameters(::titleCannotContainOnlyWhitespaces)
        validateWithParameters(::titleCannotContainBannedWords)
    }
    event(MarkComplete) {
        validate(::rateLimitCompletionStatus)
    }
    event(UnmarkComplete) {
        validate(::rateLimitCompletionStatus)
    }
    event(MoveToTrash) { }
    event(RecoverFromTrash) { }
    event(DeleteFromTrash) { }
    event(DeleteTodoInternal) { }

    voidState {
        onEvent(CreateTodo) {
            createModel(Created, ::createTodo)
        }
    }

    state(Created) {
        onEnter {
            job(::updateUserBill)
        }

        onEvent(MoveToTrash) {
            transitionTo(Trashed)
        }
        onEvent(MarkComplete) {
            transitionTo(Completed)
        }
        onEvent(DeleteTodoInternal) {
            delete()
        }
    }

    state(Completed) {
        onEvent(MoveToTrash) {
            transitionTo(Trashed)
        }
        onEvent(UnmarkComplete) {
            transitionTo(Created)
        }
        onEvent(DeleteTodoInternal) {
            delete()
        }
    }

    state(Trashed) {
        onEvent(RecoverFromTrash) {
            transitionTo(Created)
        }
        atTime(::autoDeleteTodoInTrashTime) {
            delete()
        }
        onEvent(DeleteFromTrash) {
            delete()
        }
        onEvent(DeleteTodoInternal) {
            delete()
        }
    }
}

fun updateUserBill(args: ArgForInstanceNonEvent<Todo, Ctx, Data>): List<Job<Ctx, Data>> {
    val userID = args.model.props.userID

    class Myjob(val userID: ModelID<User>) : Job<Ctx, Data>{
        override val id: JobId = -1
        override suspend fun run(jobContext: JobContext<Ctx, Data>): JobResult {
            return JobResult.Success
        }
    }
    return emptyList()
}

fun rateLimitCompletionStatus(args: ArgForInstanceEvent<Todo, Nothing?, Ctx, Data>): Validity {
    val lastStateTransitionAt = args.model.lastStateTransitionAt
    val currentTime = args.context.time
    val duration = 1.seconds
    if (currentTime - lastStateTransitionAt < duration) {
        return Validity.Invalid("You can only change the completion status every $duration.")
    }
    return Validity.Valid
}

fun titleCannotContainOnlyWhitespaces(args: ArgForVoidEvent<Todo, CreateTodoParams, Ctx, Data>): Validity {
    val titleString = args.command.params.title.value
    if (titleString.trim().isEmpty()) {
        return Validity.Invalid("Title cannot contain only whitespaces")
    }
    return Validity.Valid
}

fun titleCannotContainBannedWords(args: ArgForVoidEvent<Todo, CreateTodoParams, Ctx, Data>): Validity {
    val titleString = args.command.params.title.value
    val bannedWords = listOf("hello", "world")
    for (word in bannedWords) {
        if (titleString.lowercase().contains(word)) {
            return Validity.Invalid("Title cannot contain the banned word '$word'")
        }
    }
    return Validity.Valid
}

object CreateTodo : VoidEventWithParameters<Todo, CreateTodoParams>(Todo::class, true, CreateTodoParams::class)
class CreateTodoParams(val title: TodoTitle, val description: TodoDescription, val username: UserName, val priority: TodoPriority)
fun createTodo(args: ArgForVoidEvent<Todo, CreateTodoParams, Ctx, Data>): Todo {
    val actor = args.context.actor
    if (actor !is GroupModelIdentity) {
        throw IllegalStateException("Actor must be a GroupModelIdentity")
    }
    return Todo(
        title = args.command.params.title,
        description = args.command.params.description,
        userID = actor.id,
        priority = args.command.params.priority,
    )
}

object MarkComplete : InstanceEventNoParameters<Todo>(Todo::class, true)
object UnmarkComplete : InstanceEventNoParameters<Todo>(Todo::class, true)
object MoveToTrash : InstanceEventNoParameters<Todo>(Todo::class, true)
object RecoverFromTrash : InstanceEventNoParameters<Todo>(Todo::class, true)
object DeleteFromTrash : InstanceEventNoParameters<Todo>(Todo::class, true)
object DeleteTodoInternal : InstanceEventWithParameters<Todo, DeleteTodoInternalParams>(Todo::class, false, DeleteTodoInternalParams::class)
class DeleteTodoInternalParams // Dummy params workaround because Kotlin generics type system with respect to nullability

fun autoDeleteTodoInTrashTime(args: ArgForInstanceNonEvent<Todo, Ctx, Data>): Instant {
    return args.time.plus(1.days)
    //return args.time.plus(1.minutes)
}
