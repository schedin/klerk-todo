package se.moshicon.klerk_todo.http

import dev.klerkframework.klerk.Klerk
import io.ktor.server.routing.Route
import io.ktor.server.routing.*
import se.moshicon.klerk_todo.Ctx
import se.moshicon.klerk_todo.Data
import se.moshicon.klerk_todo.McpConfig


fun registerChatRoutes(klerk: Klerk<Ctx, Data>, mcpConfig: McpConfig): Route.() -> Unit = {
    get("/{...}") {
        print("Get with Url ${call.request}")
        //getTodos(call, klerk)
    }
}
