package se.moshicon.klerkframework.todo_app.http

import dev.klerkframework.klerk.CommandResult
import dev.klerkframework.klerk.EventWithParameters
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.ProcessingOptions
import dev.klerkframework.klerk.misc.EventParameters
import dev.klerkframework.web.EventFormTemplate
import dev.klerkframework.web.ParseResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import se.moshicon.klerkframework.todo_app.Ctx
import se.moshicon.klerkframework.todo_app.Data
import se.moshicon.klerkframework.todo_app.notes.*
import se.moshicon.klerkframework.todo_app.users.UserName
import kotlin.reflect.KProperty1


object FormTemplates {
    lateinit var createTodoTemplate: EventFormTemplate<CreateTodoParams, Ctx>
        private set

    fun init(klerk: Klerk<Ctx, Data>) {
        createTodoTemplate = EventFormTemplate(
            EventWithParameters(
                CreateTodo.id,
                EventParameters(CreateTodoParams::class)
            ),
            klerk, "todos",
        ) {
            text(CreateTodoParams::title)
            text(CreateTodoParams::description)
            populatedAfterSubmit(CreateTodoParams::username)
            remaining()
        }
    }
}

fun registerServersideRoutes(klerk: Klerk<Ctx, Data>): Route.() -> Unit = {
    FormTemplates.init(klerk)
    get("/{...}") { indexPage(call, klerk) }
    post("/todos") {
        handleCreateTodo(call, klerk)
    }
}

suspend fun handleCreateTodo(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    val context = call.context(klerk)

    val populatedAfterSubmit: Map<KProperty1<*, Any?>, UserName> =
        mapOf(CreateTodoParams::username to UserName("Alice"))
    when (val result = FormTemplates.createTodoTemplate.parse(call, populatedAfterSubmit)) {
        is ParseResult.Invalid -> EventFormTemplate.respondInvalid(result, call)
        is ParseResult.DryRun -> {
            println("ParseResult.DryRun")
            val command = Command(
                event = CreateTodo,
                model = null,
                params = result.params
            )
            when(val commandResult = klerk.handle(command, context, ProcessingOptions(result.key, dryRun = true))) {
                is CommandResult.Failure -> {
                    val problemJson = toClientJavaScriptProblemJson(commandResult.problem.toString())
                    println(problemJson)
                    call.respond(HttpStatusCode.fromValue(commandResult.problem.recommendedHttpCode),
                        problemJson)
                }
                is CommandResult.Success -> {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
        is ParseResult.Parsed -> {
            println("ParseResult.Parsed")

            val command = Command(
                event = CreateTodo,
                model = null,
                params = result.params
            )
            when(val commandResult = klerk.handle(command, context, ProcessingOptions(result.key))) {
                is CommandResult.Failure -> {
                    call.respond(HttpStatusCode.BadRequest, commandResult.problem.toString())
                }
                is CommandResult.Success -> {
                    call.respondRedirect("")
                }
            }
        }
    }
}

/**
 * Converts an error message string into a JSON object that is parsable for the klerk-web Javascript code. Replace
 * this function when klerk-web adds a built-in function to do this.
 */
private fun toClientJavaScriptProblemJson(errorString: String): String {
    return """ {"problems": [{"humanReadable": "${errorString}"}], "fieldsMustBeNull": [], "fieldsMustNotBeNull": []} """
}

suspend fun indexPage(call: ApplicationCall, klerk: Klerk<Ctx, Data>) {
    // Set user_info cookie if it doesn't exist
    if (call.request.cookies["user_info"] == null) {
        call.response.cookies.append(
            Cookie(
                name = "user_info",
                value = "Alice:admins,users",
                path = "/",
                httpOnly = false
            )
        )
    }

    val context = call.context(klerk)
    val initialValues = CreateTodoParams(
        title = TodoTitle(""),
        description = TodoDescription(""),
        priority = TodoPriority(0),
        username = UserName("Alice"),
    )

    val createTodoForm = klerk.read(context) {
        FormTemplates.createTodoTemplate.build(call, initialValues, this, translator = context.translator)
    }

    val todos = klerk.read(context) {
        listIfAuthorized(data.todos.all).map { todo ->
            val username = get(todo.props.userID).props.name.value
            todo to username
        }
    }
    call.respondHtml {
        body {
            createTodoForm.render(this)
            h2 {
                +"Todos:"
            }
            
            table {
                thead {
                    tr {
                        th { +"Title" }
                        th { +"Description" }
                        th { +"Priority" }
                        th { +"Created By" }
                        th { +"Status" }
                    }
                }
                tbody {
                    todos.forEach { (todo, username) ->
                        tr {
                            td { +todo.props.title.value }
                            td { +todo.props.description.value }
                            td { +todo.props.priority.value.toString() }
                            td { +username }
                            td { +todo.state }
                        }
                    }
                }
            }
        }
    }
}
