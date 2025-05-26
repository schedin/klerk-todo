package se.moshicon.klerkframework.todo_app

import dev.klerkframework.klerk.*
import dev.klerkframework.klerk.collection.ModelCollections
import dev.klerkframework.klerk.storage.Persistence
import dev.klerkframework.klerk.storage.SqlPersistence
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.apache.commons.dbcp2.BasicDataSource
import org.mariadb.jdbc.MariaDbDataSource
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteDataSource
import se.moshicon.klerkframework.todo_app.notes.Todo
import se.moshicon.klerkframework.todo_app.notes.todoStateMachine
import se.moshicon.klerkframework.todo_app.users.User
import se.moshicon.klerkframework.todo_app.users.userStateMachine

class Ctx(
    override val actor: ActorIdentity,
    override val auditExtra: String? = null,
    override val time: Instant = Clock.System.now(),
    override val translator: Translator = DefaultTranslator(),
) : KlerkContext {
//    fun getUserID() : ModelID<User> {
//
//    }
}

fun createConfig() = ConfigBuilder<Ctx, Data>(Data).build {
    authorization {
        apply(authorizationRules())
    }

    managedModels {
        model(Todo::class, todoStateMachine, Data.todos)
        model(User::class, userStateMachine, Data.users)
    }
//    persistence(createMariaDbPersistence())
//    persistence(createMariaDbPersistenceWithoutConnectionPool())
    persistence(createPersistence())
    contextProvider { actor -> Ctx(actor) }
}

object Data {
    val todos = ModelCollections<Todo, Ctx>()
    val users = ModelCollections<User, Ctx>()
}

private fun createPersistence(): Persistence {
    val dbFilePath =
        requireNotNull(System.getenv("DATABASE_PATH")) { "The environment variable 'DATABASE_PATH' must be set" }
    val ds =  SQLiteDataSource()
    ds.url = "jdbc:sqlite:$dbFilePath"
    return SqlPersistence(ds)
}

private val logger = LoggerFactory.getLogger("se.moshicon.klerkframework.todo_app.Config")

private fun createMariaDbPersistenceWithoutConnectionPool(): Persistence {
    val ds = MariaDbDataSource()
    ds.setUrl("jdbc:mariadb://localhost:3306/klerk-todo")
    ds.user = "root"
    ds.setPassword("")
    return SqlPersistence(ds)
}

private fun createMariaDbPersistence(): Persistence {
    logger.info("Setting up MariaDB connection pool")

    val dataSource = BasicDataSource().apply {
        // Set the database driver and connection properties
        driverClassName = "org.mariadb.jdbc.Driver"
        url = "jdbc:mariadb://localhost:3306/klerk-todo"
        username = "root"
        password = ""

        // Connection pool configuration
        initialSize = 5 // Initial number of connections in the pool
        maxTotal = 20 // Maximum number of active connections
        maxIdle = 10 // Maximum number of idle connections
        minIdle = 5 // Minimum number of idle connections
        maxWaitMillis = 10000 // Maximum time to wait for a connection (10 seconds)

        // Connection validation and maintenance
        testOnBorrow = true
        validationQuery = "SELECT 1"
        validationQueryTimeout = 3 // Validation timeout in seconds

        // Connection pool maintenance
        timeBetweenEvictionRunsMillis = 60000 // Run the evictor every minute
        minEvictableIdleTimeMillis = 300000 // Minimum time a connection can be idle before eviction (5 minutes)
    }

    logger.info("MariaDB connection pool initialized with maxTotal={}, initialSize={}", dataSource.maxTotal, dataSource.initialSize)
    return SqlPersistence(dataSource)
}