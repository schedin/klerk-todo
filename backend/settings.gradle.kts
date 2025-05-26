rootProject.name = "todo-app-backend"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

includeBuild("../../../klerk-mcp") {
//    dependencySubstitution {
//        substitute(module("dev.klerkframework:klerk-mcp"))
//            .using(project(":"))
//    }
}