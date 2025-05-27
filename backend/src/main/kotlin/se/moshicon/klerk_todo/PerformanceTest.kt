package se.moshicon.klerk_todo

import dev.klerkframework.klerk.AuthenticationIdentity
import dev.klerkframework.klerk.Klerk
import dev.klerkframework.klerk.command.Command
import dev.klerkframework.klerk.command.CommandToken
import dev.klerkframework.klerk.command.ProcessingOptions
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import se.moshicon.klerk_todo.notes.*
import se.moshicon.klerk_todo.users.GroupModelIdentity
import se.moshicon.klerk_todo.users.UserName
import kotlin.time.measureTime

private val logger = LoggerFactory.getLogger("se.moshicon.klerk_todo.PerformanceTest")

fun performanceInsertTest(klerk: Klerk<Ctx, Data>) {
    logger.info("Starting performance insert test...")

    // Simple performance test
//    runBlocking {
//        val aliceUser = klerk.read(Ctx(AuthenticationIdentity)) {
//            getFirstWhere(data.users.all) { it.props.name.value == "Alice" }
//        }
//
//        for (i in 0 until 10) {
//            val context = Ctx(GroupModelIdentity(model = aliceUser, groups = listOf("admins", "users")))
//            val command = Command(
//                event = CreateTodo,
//                model = null,
//                params = CreateTodoParams(
//                    title = TodoTitle("test Title"),
//                    description = TodoDescription("test desc"),
//                    username = UserName("Alice"),
//                    priority = TodoPriority(4),
//                ),
//            )
//            val result = klerk.handle(command, context, ProcessingOptions(CommandToken.simple()))
//        }
//    }

    runBlocking {
        val aliceUser = klerk.read(Ctx(AuthenticationIdentity)) {
            getFirstWhere(data.users.all) { it.props.name.value == "Alice" }
        }
        val totalTime = measureTime {
            for (j in 0 until 100) {
                val seconds = measureTime {
                    for (i in 0 until 10000) {
                        val context = Ctx(GroupModelIdentity(model = aliceUser, groups = listOf("admins", "users")))
                        val command = Command(
                            event = CreateTodo,
                            model = null,
                            params = CreateTodoParams(
                                title = TodoTitle("test Title"),
                                description = TodoDescription("test desc"),
                                username = UserName("Alice"),
                                priority = TodoPriority(4),
                            ),
                        )
                        val result = klerk.handle(command, context, ProcessingOptions(CommandToken.simple()))
//            println(result)
//            delay(1)
                    }
                }.inWholeSeconds
                println("10000 commands took $seconds seconds")
            }
        }.inWholeSeconds
        println("100 iterations of 10000 commands took $totalTime seconds")

    }
}