import React, { useState, useEffect, useRef } from 'react';
import { ChatMessage as ChatMessageType } from '../types/chat';
import { chatApi } from '../services/chatApi';
import ChatMessage from './ChatMessage';

interface ChatDialogProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: string;
}

const ChatDialog: React.FC<ChatDialogProps> = ({ isOpen, onClose, currentUser }) => {
  const [messages, setMessages] = useState<ChatMessageType[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Scroll to bottom when new messages arrive
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Load chat history when dialog opens and focus input
  useEffect(() => {
    if (isOpen) {
      loadHistory();
      // Focus the input field when dialog opens
      setTimeout(() => {
        inputRef.current?.focus();
      }, 100);
    }
  }, [isOpen]);

  const loadHistory = async () => {
    try {
      setError(null);
      const history = await chatApi.getHistory();
      setMessages(history);
    } catch (err) {
      console.error('Failed to load chat history:', err);
      setError('Failed to load chat history');
    }
  };

  const sendMessage = async () => {
    if (!inputValue.trim() || loading) return;

    const messageContent = inputValue.trim();
    setInputValue('');
    setLoading(true);
    setError(null);

    try {
      // Add user message to UI immediately
      const userMessage: ChatMessageType = {
        id: `temp-${Date.now()}`,
        content: messageContent,
        timestamp: Math.floor(Date.now() / 1000),
      };
      setMessages(prev => [...prev, userMessage]);

      // Send message to server
      await chatApi.sendMessage(messageContent);

      // Reload history to get both user message and response with correct IDs
      await loadHistory();
    } catch (err) {
      console.error('Failed to send message:', err);
      setError('Failed to send message');
      // Remove the temporary user message on error
      setMessages(prev => prev.filter(msg => !msg.id.startsWith('temp-')));
    } finally {
      setLoading(false);
      // Refocus the input field after sending
      setTimeout(() => {
        inputRef.current?.focus();
      }, 100);
    }
  };

  const clearHistory = async () => {
    try {
      setError(null);
      await chatApi.clearHistory();
      setMessages([]);
    } catch (err) {
      console.error('Failed to clear chat history:', err);
      setError('Failed to clear chat history');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '80px',
        right: '20px',
        width: '350px',
        height: '500px',
        backgroundColor: 'white',
        borderRadius: '12px',
        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.15)',
        display: 'flex',
        flexDirection: 'column',
        zIndex: 1000,
        border: '1px solid #e0e0e0',
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: '16px',
          borderBottom: '1px solid #e0e0e0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: '#f8f9fa',
          borderRadius: '12px 12px 0 0',
        }}
      >
        <h3 style={{ margin: 0, fontSize: '16px', fontWeight: '600' }}>
          🤖 Chat Assistant
        </h3>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            onClick={clearHistory}
            style={{
              backgroundColor: '#ff9800',
              color: 'white',
              border: 'none',
              padding: '6px 12px',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '12px',
            }}
          >
            Clear
          </button>
          <button
            onClick={onClose}
            style={{
              backgroundColor: '#f44336',
              color: 'white',
              border: 'none',
              padding: '6px 12px',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '12px',
            }}
          >
            ✕
          </button>
        </div>
      </div>

      {/* Messages */}
      <div
        style={{
          flex: 1,
          padding: '16px',
          overflowY: 'auto',
          backgroundColor: '#fafafa',
        }}
      >
        {error && (
          <div
            style={{
              backgroundColor: '#ffebee',
              color: '#c62828',
              padding: '8px 12px',
              borderRadius: '6px',
              marginBottom: '12px',
              fontSize: '14px',
            }}
          >
            {error}
          </div>
        )}

        {messages.length === 0 ? (
          <div
            style={{
              textAlign: 'center',
              color: '#666',
              fontSize: '14px',
              marginTop: '20px',
            }}
          >
            Start a conversation! You can ask me to help with your TODOs.
          </div>
        ) : (
          messages.map((message, index) => {
            // Determine if this is a user message (odd indices are typically user messages)
            // This is a simple heuristic - in a real app you'd have a proper way to identify user vs bot messages
            const isUser = index % 2 === 0;
            return (
              <ChatMessage
                key={message.id}
                message={message}
                isUser={isUser}
              />
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div
        style={{
          padding: '16px',
          borderTop: '1px solid #e0e0e0',
          backgroundColor: 'white',
          borderRadius: '0 0 12px 12px',
        }}
      >
        <div style={{ display: 'flex', gap: '8px' }}>
          <input
            ref={inputRef}
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Type your message..."
            disabled={loading}
            style={{
              flex: 1,
              padding: '10px 12px',
              border: '1px solid #ddd',
              borderRadius: '20px',
              outline: 'none',
              fontSize: '14px',
            }}
          />
          <button
            onClick={sendMessage}
            disabled={!inputValue.trim() || loading}
            style={{
              backgroundColor: inputValue.trim() && !loading ? '#2196F3' : '#ccc',
              color: 'white',
              border: 'none',
              padding: '10px 16px',
              borderRadius: '20px',
              cursor: inputValue.trim() && !loading ? 'pointer' : 'not-allowed',
              fontSize: '14px',
            }}
          >
            {loading ? '...' : 'Send'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChatDialog;
