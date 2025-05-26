val klerk_version: String by project
val klerk_web_version: String by project
val ktor_version: String by project
val logback_version: String by project
val sqlite_jdbc_version: String by project
val coroutinesVersion: String by project
val jwt_version: String by project
val commons_dbcp2_version = "2.11.0"


plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "2.3.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.expediagroup.graphql") version "7.1.1"
}

group = "se.moshicon.klerkframework.todo_app"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("se.moshicon.klerkframework.todo_app.ApplicationKt")
}

dependencies {
    implementation("com.github.klerk-framework:klerk:$klerk_version")
    implementation("com.github.klerk-framework:klerk-web:$klerk_web_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.xerial:sqlite-jdbc:$sqlite_jdbc_version")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.1")
    implementation("org.apache.commons:commons-dbcp2:$commons_dbcp2_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-sse:${ktor_version}")
    implementation("io.ktor:ktor-server-sse:${ktor_version}")
    implementation("io.ktor:ktor-server-sse-jvm:${ktor_version}")

    // Authentication
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("com.auth0:java-jwt:$jwt_version")

    implementation("dev.klerkframework:klerk-mcp:0.1.0-SNAPSHOT") //TODO, fix version before release
    implementation("io.modelcontextprotocol:kotlin-sdk:${property("mcp_sdk_version")}") // MCP SDK

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
