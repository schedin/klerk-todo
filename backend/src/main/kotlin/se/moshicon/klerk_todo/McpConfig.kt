package se.moshicon.klerk_todo

/**
 * Configuration constants for the MCP (Model Context Protocol) server.
 *
 * The MCP server runs separately from the main API server to provide
 * AI tools and resources for external clients.
 */
object McpConfig {
    /** Host address for the MCP server */
    const val HOST = "0.0.0.0"

    /** Port number for the MCP server */
    const val PORT = 8081

    /** Full URL for the MCP server */
    const val URL = "http://localhost:$PORT"

    /** Server name for MCP identification */
    const val SERVER_NAME = "TODO application"

    /** Server version for MCP identification */
    const val SERVER_VERSION = "1.0.0"
}
