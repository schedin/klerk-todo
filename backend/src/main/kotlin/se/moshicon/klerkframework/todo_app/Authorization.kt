package se.moshicon.klerkframework.todo_app

import dev.klerkframework.klerk.*
import se.moshicon.klerkframework.todo_app.notes.CreateTodo
import se.moshicon.klerkframework.todo_app.notes.CreateTodoParams
import se.moshicon.klerkframework.todo_app.notes.Todo
import se.moshicon.klerkframework.todo_app.users.GroupModelIdentity
import se.moshicon.klerkframework.todo_app.users.User
import dev.klerkframework.klerk.PositiveAuthorization.*
import se.moshicon.klerkframework.todo_app.notes.DeleteTodoInternal
import se.moshicon.klerkframework.todo_app.users.CreateUser

private const val USERS_GROUP = "users"
private const val ADMINS_GROUP = "admins"
private const val GUESTS_GROUP = "guests"

fun authorizationRules(): ConfigBuilder.AuthorizationRulesBlock<Ctx, Data>.() -> Unit = {
    commands {
        positive {
            rule(::userCanCreateOwnTodos)
            rule(::userCanModifyOwnTodos)
            rule(::authenticationIdentityCanModifyUsers)
            rule(::authenticationIdentityCanDeleteTodos)
        }
        negative {
            rule(::guestsCanOnlyCreateOneTodo)
        }
    }
    readModels {
        positive {
            rule(::unAuthenticatedCanReadUsers)
            rule(::authenticationIdentityCanReadUsers)
            rule(::userCanReadOwnTodos)
            rule(::userCanReadOwnUser)
            rule(::adminGroupCanReadAllTodos)
            rule(::adminGroupCanReadAllUsers)
        }
        negative {
        }
    }
    readProperties {
        positive {
            rule(::authenticationIdentityCanReadUsersProps)
            rule(::userCanReadOwnTodoProps)
            rule(::userCanReadOwnUserProps)
            rule(::unAuthenticatedCanReadUsersProps)
            rule(::adminGroupCanReadAllTodoProps)
            rule(::adminGroupCanReadAllUserProps)
        }
        negative {
        }
    }
    eventLog {
        positive {
            rule(::allCanReadEventLog)
        }
        negative {
        }
    }
}

/**
 * Normally you should have some protection to be able to modify users. But since the browser is simulating the IdP,
 * we allow it.
 */
fun authenticationIdentityCanModifyUsers(args: ArgCommandContextReader<*, Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    if (actor !is AuthenticationIdentity) {
        return NoOpinion
    }

    if (args.command.event == CreateUser) {
        return Allow
    }

    val commandModelID = args.command.model ?: return NoOpinion
    val model = args.reader.get(commandModelID)
    val domainModel = model.props
    return if (domainModel is User) Allow else NoOpinion
}

fun allCanReadEventLog(@Suppress("UNUSED_PARAMETER") args: ArgContextReader<Ctx, Data>): PositiveAuthorization {
    return Allow
}

fun userCanReadOwnUserProps(args: ArgsForPropertyAuth<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val user = args.model.props
    if (actor is GroupModelIdentity && user is User && user == actor.model.props) {
        return Allow
    }
    return NoOpinion
}

fun userCanReadOwnTodoProps(args: ArgsForPropertyAuth<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val todo = args.model.props

    if (actor !is GroupModelIdentity || todo !is Todo) {
        return NoOpinion
    }

    return if (todo.userID == actor.id) Allow else NoOpinion
}

fun userCanCreateOwnTodos(args: ArgCommandContextReader<*, Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val createParams = args.command.params
    if (actor is GroupModelIdentity &&
        (actor.groups.contains(USERS_GROUP) || actor.groups.contains(GUESTS_GROUP)) &&
        args.command.event is CreateTodo &&
        createParams is CreateTodoParams &&
        createParams.username == actor.model.props.name
    ) {
        return Allow
    }
    return NoOpinion
}

@HumanReadable("You can only create one TODO as guest. Please buy premium.")
fun guestsCanOnlyCreateOneTodo(args: ArgCommandContextReader<*, Ctx, Data>): NegativeAuthorization {
    val actor = args.context.actor
    if (actor is GroupModelIdentity && actor.groups.contains(GUESTS_GROUP) && !actor.groups.contains(USERS_GROUP)
        && args.command.event is CreateTodo) {
        val numOfTodosForUser = args.reader.list(args.reader.data.todos.all) {
            actor.model.id == it.props.userID
        }.size
        return if (numOfTodosForUser < 1) NegativeAuthorization.Pass else NegativeAuthorization.Deny
    }
    return NegativeAuthorization.Pass
}

fun userCanModifyOwnTodos(args: ArgCommandContextReader<*, Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val commandModelID = args.command.model
    if (actor !is GroupModelIdentity || commandModelID == null) {
        return NoOpinion
    }
    val todoModel = args.reader.get(commandModelID)
    val todo = todoModel.props

    return if (todo is Todo && todo.userID == actor.model.id) Allow else NoOpinion
}


fun authenticationIdentityCanDeleteTodos(args: ArgCommandContextReader<*, Ctx, Data>): PositiveAuthorization {
    val commandModelID = args.command.model
    if (commandModelID != null && args.context.actor == AuthenticationIdentity && args.command.event == DeleteTodoInternal) {
        return Allow
    }
    return NoOpinion
}


fun unAuthenticatedCanReadUsers(args: ArgModelContextReader<Ctx, Data>): PositiveAuthorization {
    if (args.context.actor == Unauthenticated && args.model.props is User) {
        return Allow
    }
    return NoOpinion
}

fun unAuthenticatedCanReadUsersProps(args: ArgsForPropertyAuth<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val user = args.model.props
    if (actor is Unauthenticated && user is User) {
        return Allow
    }
    return NoOpinion
}

fun adminGroupCanReadAllTodoProps(args: ArgsForPropertyAuth<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val todo = args.model.props
    if (actor is GroupModelIdentity && actor.groups.contains(ADMINS_GROUP) && todo is Todo) {
        return Allow
    }
    return NoOpinion
}

fun adminGroupCanReadAllUserProps(args: ArgsForPropertyAuth<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    if (actor is GroupModelIdentity && actor.groups.contains(ADMINS_GROUP) && args.model.props is User) {
        return Allow
    }
    return NoOpinion
}

fun authenticationIdentityCanReadUsers(args: ArgModelContextReader<Ctx, Data>): PositiveAuthorization {
    if (args.context.actor == AuthenticationIdentity && args.model.props is User) {
        return Allow
    }
    return NoOpinion
}

fun authenticationIdentityCanReadUsersProps(args: ArgsForPropertyAuth<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val user = args.model.props
    if (actor is AuthenticationIdentity && user is User) {
        return Allow
    }
    return NoOpinion
}

fun userCanReadOwnUser(args: ArgModelContextReader<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val userModel = args.model
    if (actor is GroupModelIdentity && userModel.props is User && userModel.id == actor.id) {
        return Allow
    }
    return NoOpinion
}

fun userCanReadOwnTodos(args: ArgModelContextReader<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val todo = args.model.props
    if (actor is GroupModelIdentity && todo is Todo && todo.userID == actor.id) {
        return Allow
    }
    return NoOpinion
}

fun adminGroupCanReadAllTodos(args: ArgModelContextReader<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    val todo = args.model.props
    if (actor is GroupModelIdentity && todo is Todo && actor.groups.contains(ADMINS_GROUP)) {
        return Allow
    }
    return NoOpinion
}

fun adminGroupCanReadAllUsers(args: ArgModelContextReader<Ctx, Data>): PositiveAuthorization {
    val actor = args.context.actor
    if (actor is GroupModelIdentity && args.model.props is User && actor.groups.contains(ADMINS_GROUP)) {
        return Allow
    }
    return NoOpinion
}
