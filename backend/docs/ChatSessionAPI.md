# Chat Session API Documentation

## Overview

The Chat Session API provides server-side session management for chat conversations. Each user's chat state is maintained in server memory with automatic cleanup of old sessions. Session handling is completely transparent to the browser - users are identified by their JWT token and sessions are managed automatically.

## Features

- **Server-side session storage**: Chat history is kept in server memory
- **User-based sessions**: Each authenticated user gets their own session (one per user)
- **Automatic cleanup**: Sessions older than 30 minutes are automatically removed
- **JWT Authentication**: All endpoints require valid JWT authentication
- **Thread-safe**: Uses concurrent data structures for safe multi-user access
- **Transparent session handling**: No session IDs exposed to the browser

## API Endpoints

All endpoints are under `/api/chat/` and require JWT authentication.

### GET `/api/chat/history`
Gets the chat history for the authenticated user. Creates a session automatically if none exists.

**Response:**
```json
{
  "messages": [
    {
      "id": "message-uuid",
      "content": "Hello!",
      "timestamp": 1640995200
    }
  ]
}
```

### POST `/api/chat/message`
Sends a message to the user's chat session. Creates a session automatically if none exists.

**Request Body:**
```json
{
  "content": "Hello, how are you?"
}
```

**Response:**
```json
{
  "message": {
    "id": "message-uuid",
    "content": "Hello, how are you?",
    "timestamp": 1640995200
  }
}
```

### DELETE `/api/chat/history`
Clears the chat history for the authenticated user.

**Response:**
```json
{
  "message": "Chat history cleared successfully"
}
```

### GET `/api/chat/stats`
Gets statistics about all chat sessions (for monitoring/debugging).

**Response:**
```json
{
  "totalSessions": 5,
  "activeSessions": 3,
  "totalMessages": 42,
  "oldestSessionAge": 15
}
```

## Session Management

### Session Lifecycle
1. **Creation**: Sessions are created automatically when a user first sends a message or requests history
2. **Access**: Sessions are updated with `lastAccessedAt` timestamp on every interaction
3. **Expiration**: Sessions expire after 30 minutes of inactivity
4. **Cleanup**: A background process removes expired sessions every 5 minutes

### Session Storage
- Sessions are stored in server memory using `ConcurrentHashMap`
- Each user can have only one active session at a time
- Sessions are identified internally by UUID but linked to user ID from JWT token
- No session identifiers are exposed to the browser

## Error Handling

All endpoints return appropriate HTTP status codes:

- `200 OK`: Successful operation
- `400 Bad Request`: Invalid request data (e.g., empty message content)
- `401 Unauthorized`: Missing or invalid JWT token
- `404 Not Found`: Session not found
- `500 Internal Server Error`: Server-side error

Error responses include a JSON object with an `error` field:
```json
{
  "error": "Message content cannot be empty"
}
```

## Authentication

All endpoints require a valid JWT token in the `Authorization` header:
```
Authorization: Bearer <jwt-token>
```

The JWT token must contain a `sub` claim with the user ID.

## Configuration

Session management configuration is in `ChatSessionManager`:
- `SESSION_TIMEOUT_MINUTES`: 30 minutes (session expiration time)
- `CLEANUP_INTERVAL_MINUTES`: 5 minutes (cleanup frequency)

## Example Usage

1. **Send a message:**
   ```bash
   curl -X POST \
        -H "Authorization: Bearer <token>" \
        -H "Content-Type: application/json" \
        -d '{"content": "Hello!"}' \
        http://localhost:8080/api/chat/message
   ```

2. **Get chat history:**
   ```bash
   curl -H "Authorization: Bearer <token>" \
        http://localhost:8080/api/chat/history
   ```

3. **Clear chat history:**
   ```bash
   curl -X DELETE \
        -H "Authorization: Bearer <token>" \
        http://localhost:8080/api/chat/history
   ```

4. **Get session statistics:**
   ```bash
   curl -H "Authorization: Bearer <token>" \
        http://localhost:8080/api/chat/stats
   ```
