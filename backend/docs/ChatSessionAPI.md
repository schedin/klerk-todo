# Chat Session API Documentation

## Overview

The Chat Session API provides server-side session management for chat conversations. Each user's chat state is maintained in server memory with automatic cleanup of old sessions.

## Features

- **Server-side session storage**: Chat history is kept in server memory
- **User-based sessions**: Each authenticated user gets their own session
- **Automatic cleanup**: Sessions older than 30 minutes are automatically removed
- **JWT Authentication**: All endpoints require valid JWT authentication
- **Thread-safe**: Uses concurrent data structures for safe multi-user access

## API Endpoints

All endpoints are under `/api/chat/` and require JWT authentication.

### GET `/api/chat/session`
Gets or creates a chat session for the authenticated user.

**Response:**
```json
{
  "sessionId": "uuid-string",
  "messages": [],
  "createdAt": 1640995200,
  "lastAccessedAt": 1640995200
}
```

### GET `/api/chat/history`
Gets the chat history for the authenticated user's session.

**Response:**
```json
{
  "sessionId": "uuid-string",
  "messages": [
    {
      "id": "message-uuid",
      "content": "Hello!",
      "role": "USER",
      "timestamp": 1640995200
    }
  ],
  "createdAt": 1640995200,
  "lastAccessedAt": 1640995200
}
```

### POST `/api/chat/message`
Sends a message to the user's chat session.

**Request Body:**
```json
{
  "content": "Hello, how are you?",
  "role": "USER"
}
```

**Response:**
```json
{
  "message": {
    "id": "message-uuid",
    "content": "Hello, how are you?",
    "role": "USER",
    "timestamp": 1640995200
  },
  "sessionId": "session-uuid"
}
```

### DELETE `/api/chat/session`
Clears the chat history for the authenticated user's session.

**Response:**
```json
{
  "message": "Session cleared successfully"
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

## Message Roles

- `USER`: Messages sent by the user
- `ASSISTANT`: Messages sent by the AI assistant
- `SYSTEM`: System messages

## Session Management

### Session Lifecycle
1. **Creation**: Sessions are created automatically when a user first accesses the chat
2. **Access**: Sessions are updated with `lastAccessedAt` timestamp on every interaction
3. **Expiration**: Sessions expire after 30 minutes of inactivity
4. **Cleanup**: A background process removes expired sessions every 5 minutes

### Session Storage
- Sessions are stored in server memory using `ConcurrentHashMap`
- Each user can have only one active session at a time
- Sessions are identified by UUID and linked to user ID from JWT token

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

## Testing

To test the chat session functionality, you can run the manual test:

```kotlin
// In your application or test environment
ChatSessionTest.runAllTests()
```

This will verify all core functionality including session creation, message handling, and cleanup.

## Example Usage

1. **Start a chat session:**
   ```bash
   curl -H "Authorization: Bearer <token>" \
        http://localhost:8080/api/chat/session
   ```

2. **Send a message:**
   ```bash
   curl -X POST \
        -H "Authorization: Bearer <token>" \
        -H "Content-Type: application/json" \
        -d '{"content": "Hello!", "role": "USER"}' \
        http://localhost:8080/api/chat/message
   ```

3. **Get chat history:**
   ```bash
   curl -H "Authorization: Bearer <token>" \
        http://localhost:8080/api/chat/history
   ```

4. **Clear session:**
   ```bash
   curl -X DELETE \
        -H "Authorization: Bearer <token>" \
        http://localhost:8080/api/chat/session
   ```
