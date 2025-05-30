# klerk-todo

A TODO management system demonstrating [Klerk Framework](https://github.com/klerk-framework/klerk). It also features an auto-generated MCP (Model Context Protocol) server and chat interface with an MCP client and LLM integration.

## 🏗️ Architecture Overview

This application showcases an event-driven architecture built on the Klerk Framework.

### Core Components
- **Klerk Framework**: Provides the foundational event-driven architecture with state machines, models, and business logic
- **Auto-generated MCP Server**: Automatically generated from Klerk models and state machine definitions
- **AI Chat Interface**: LLM-powered chat backed by MCP client for natural language TODO management

## 🛠️ Technology Stack

### Backend
- **Kotlin** with **Klerk Framework**
- **Ktor** for HTTP server and SSE (Server-Sent Events)
- **MCP (Model Context Protocol)** for AI integration
- **JWT Authentication validation**
- **SQLite** for persistence

### Frontend
- **React 19** with **TypeScript**
- **JWT-based Authentication**, simulating an identity server, but in the web browser.

### AI Integration
- **Kotlin MCP SDK** for protocol implementation
- **OpenAI Java/Kotlin Client** for LLM communication
- **Ollama Support** for local LLM deployment

## 🚀 Quick Start

### Prerequisites
- **Java 21+**
- **Node.js 18+**
- **LLM Server** (Ollama recommended for local development)

### Backend Setup
```bash
cd backend
./gradlew run
```

### Frontend Setup
```bash
cd frontend
npm install
npm start
```

### LLM Setup (Ollama)
```bash
# Install Ollama and pull a model
ollama pull qwen3:0.6b
ollama serve
```

The application will be available at:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **MCP Server**: http://localhost:8080/mcp (SSE endpoint)
