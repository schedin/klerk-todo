package se.moshicon.klerk_todo

/**
 * Configuration constants for the MCP (Model Context Protocol) server.
 */
object McpServerConfig {
    /** Host address for the MCP server */
    const val HOST = "0.0.0.0"

    /** Port number for the MCP server */
    const val PORT = 8081

    /** Full URL for the MCP server */
    private const val URL = "http://127.0.0.1:$PORT"

    /** Server name for MCP identification */
    const val SERVER_NAME = "TODO application"

    /** Server version for MCP identification */
    const val SERVER_VERSION = "1.0.0"

    fun getServerUrl(): String {
        return URL
    }
}

/* Configuration object for MCP clients */
object McpClientConfig {
    private const val DEFAULT_LLM_URL = "http://localhost:11434/v1"
    private const val DEFAULT_LLM_MODEL = "gemma3:1b-it-qat"

    /**
     * LLM server URL that MCP client should use.
     */
    val llmServerUrl: String
        get() = System.getenv("LLM_URL") ?: DEFAULT_LLM_URL

    /**
     * LLM model name to use for chat completions.
     */
    val llmModel: String
        get() = System.getenv("LLM_MODEL") ?: DEFAULT_LLM_MODEL

    /** The MCP server URL for the client to connect to */
    val mcpServerUrl: String
        get() = McpServerConfig.getServerUrl()
}
