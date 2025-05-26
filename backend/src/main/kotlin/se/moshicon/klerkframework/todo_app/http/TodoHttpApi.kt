package se.moshicon.klerkframework.todo_app.http

import dev.klerkframework.klerk.AuthenticationIdentity
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.Model
import dev.klerkframework.klerk.ModelID
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import dev.klerkframework.klerk.CommandResult.Success
import dev.klerkframework.klerk.CommandResult.Failure
import dev.klerkframework.klerk.InstanceEventNoParameters
import org.slf4j.LoggerFactory

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import se.moshicon.klerkframework.todo_app.*
import se.moshicon.klerkframework.todo_app.notes.*
import se.moshicon.klerkframework.todo_app.users.GroupModelIdentity

private val logger = LoggerFactory.getLogger("se.moshicon.klerkframework.todo_app.http.TodoHttpApi")

/**
 * Cache for TODO model IDs to improve performance of the random TODO endpoint
 */
object TodoCache {
    private var todoModelIds = listOf<ModelID<Todo>>()
    private var lastRefreshTime = 0L
    private const val CACHE_TTL_MS = 60000 // 1 minute cache TTL

    suspend fun refreshIfNeeded(klerk: Klerk<Ctx, Data>, context: Ctx) {
        val currentTime = System.currentTimeMillis()
        if (todoModelIds.isEmpty() || currentTime - lastRefreshTime > CACHE_TTL_MS) {
            logger.debug("Refreshing TODO model ID cache")
            todoModelIds = klerk.read(context) {
                listIfAuthorized(data.todos.all).map { it.id }
            }
            lastRefreshTime = currentTime
            logger.debug("Cache refreshed with ${todoModelIds.size} TODOs")
        }
    }

    /**
     * Gets a random TODO model ID from the cache
     * @return A random TODO model ID or null if the cache is empty
     */
    fun getRandomTodoId(): ModelID<Todo>? {
        return if (todoModelIds.isNotEmpty()) {
            todoModelIds.random()
        } else {
            null
        }
    }
}

fun registerTodoRoutes(klerk: Klerk<Ctx, Data>): Route.() -> Unit = {
    get("/{...}") {
        getTodos(call, klerk)
    }
    get("/random") {
        getRandomTodo(call, klerk)
    }
    post("/{...}") {
        createTodo(call, klerk)
    }
    post("/{todoID}/trash") {
        trash(call, klerk)
    }
    post("/{todoID}/untrash") {
        unTrash(call, klerk)
    }
    post("/{todoID}/complete") {
        markComplete(call, klerk)
    }
    post("/{todoID}/uncomplete") {
        markUncomplete(call, klerk)
    }
    delete("/{todoID}") {
        delete(call, klerk)
    }
}

@Serializable
data class TodoResponse(
    val todoID: String,
    val title: String,
    val description: String,
    val state: String,
    val createdAt: Instant,
    val username: String,
    val priority: Int,
)

@Serializable
data class CreateTodoRequest(val title: String, val description: String, val priority: Int)

private fun toTodoResponse(todo: Model<Todo>, username: String) = TodoResponse(
    todoID = todo.id.toString(),
    title = todo.props.title.value,
    description = todo.props.description.value,
    state = todo.state,
    createdAt = todo.createdAt,
    username = username,
    priority = todo.props.priority.value,
)

suspend fun getTodos(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    val context = call.context(klerk)
    val todos = klerk.read(context) {
        listIfAuthorized(data.todos.all).map { todo ->
            val username = get(todo.props.userID).props.name.value
            toTodoResponse(todo, username)
        }
    }
    call.respond(todos)
}

suspend fun createTodo(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    val context = call.context(klerk)
    val params = call.receive<CreateTodoRequest>()

    val user = (context.actor as? GroupModelIdentity)?.model?.props
        ?: throw IllegalStateException("User not found in context")

    val command = Command(
        event = CreateTodo,
        model = null,
        params = CreateTodoParams(
            title = TodoTitle(params.title),
            description = TodoDescription(params.description),
            username = user.name,
            priority = TodoPriority(params.priority),
        ),
    )
    when(val result = klerk.handle(command, context, ProcessingOptions(CommandToken.simple()))) {
        is Failure -> {
            call.respond(HttpStatusCode.BadRequest, result.problem.toString())
        }
        is Success -> {
            val createdTodo = klerk.read(context) {
                get(result.primaryModel!!)
            }

            // Refresh the TODO cache to include the newly created TODO
            TodoCache.refreshIfNeeded(klerk, context)

            call.respond(HttpStatusCode.Created, toTodoResponse(createdTodo, user.name.value))
        }
    }
}

suspend fun handleTodoCommand(
    call: ApplicationCall,
    klerk: Klerk<Ctx, Data>,
    event: InstanceEventNoParameters<Todo>,
    onSuccess: suspend (pair: Pair<Model<Todo>, String>) -> Unit = { },
) {
    val todoID = call.parameters["todoID"] ?: throw IllegalArgumentException("todoID is required")
    val context = call.context(klerk)
    val todo = klerk.read(context) {
        getFirstWhere(data.todos.all) { it.id.toString() == todoID }
    }
    val command = Command(
        event = event,
        model = todo.id,
        params = null
    )
    when(val result = klerk.handle(command, context, ProcessingOptions(CommandToken.simple()))) {
        is Failure -> call.respond(HttpStatusCode.BadRequest, result.problem.toString())
        is Success -> {
            val modifiedTodo = klerk.read(context) {
                getOrNull(result.primaryModel!!)
            } ?: todo

            val user = klerk.read(context) {
                get(modifiedTodo.props.userID)
            }
            onSuccess(modifiedTodo to user.props.name.value)
        }
    }
}

suspend fun markComplete(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    handleTodoCommand(call, klerk, MarkComplete) { (todo, username) ->
        call.respond(HttpStatusCode.Created, toTodoResponse(todo, username))
    }
}

suspend fun markUncomplete(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    handleTodoCommand(call, klerk, UnmarkComplete) { (todo, username) ->
        call.respond(HttpStatusCode.Created, toTodoResponse(todo, username))
    }
}

suspend fun delete(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    val context = call.context(klerk)
    handleTodoCommand(call, klerk, DeleteFromTrash) {
        // Refresh the TODO cache after deletion
        TodoCache.refreshIfNeeded(klerk, context)
        call.respond(HttpStatusCode.NoContent)
    }
}

suspend fun trash(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    handleTodoCommand(call, klerk, MoveToTrash) { (todo, username) ->
        call.respond(HttpStatusCode.OK, toTodoResponse(todo, username))
    }
}

suspend fun unTrash(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    handleTodoCommand(call, klerk, RecoverFromTrash) { (todo, username) ->
        call.respond(HttpStatusCode.OK, toTodoResponse(todo, username))
    }
}

/** Handles the /todos/random endpoint.  Returns a random entry from the database */
suspend fun getRandomTodo(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    val context = call.context(klerk)
    TodoCache.refreshIfNeeded(klerk, context)
    val randomTodoId = TodoCache.getRandomTodoId()
    if (randomTodoId == null) {
        call.respond(HttpStatusCode.NotFound, "No TODOs available")
        return
    }
    val todo = klerk.read(context) {
        val todo = get(randomTodoId)
        val username = get(todo.props.userID).props.name.value
        toTodoResponse(todo, username)
    }
    call.respond(todo)
}
