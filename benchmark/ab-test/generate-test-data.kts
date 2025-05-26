#!/usr/bin/env kotlin

import java.io.File
import kotlin.random.Random

// Configuration
val outputDir = "../build/test-data"
val numFiles = 100
val priorities = listOf(0, 2, 4, 6, 8, 10) // Only even priorities are allowed

// Create output directory if it doesn't exist
File(outputDir).mkdirs()

// Generate random todo data
repeat(numFiles) { index ->
    val title = "Benchmark Todo #${index + 1}"
    val description = "This is a benchmark todo with index ${index + 1} created at ${System.currentTimeMillis()}"
    val priority = priorities[Random.nextInt(priorities.size)]

    val json = """
    {
      "title": "$title",
      "description": "$description",
      "priority": $priority
    }
    """.trimIndent()

    File("$outputDir/todo-$index.json").writeText(json)
    println("Generated $outputDir/todo-$index.json")
}

println("Generated $numFiles test data files in $outputDir directory")
