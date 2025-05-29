import React from 'react';
import { ChatMessage as ChatMessageType, MessageSender } from '../types/chat';

interface ChatMessageProps {
  message: ChatMessageType;
}

const ChatMessage: React.FC<ChatMessageProps> = ({ message }) => {
  const isUser = message.sender === MessageSender.USER;
  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp * 1000);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        marginBottom: '12px',
      }}
    >
      <div
        style={{
          maxWidth: '80%',
          padding: '8px 12px',
          borderRadius: '12px',
          backgroundColor: isUser ? '#2196F3' : '#f1f1f1',
          color: isUser ? 'white' : 'black',
          wordWrap: 'break-word',
        }}
      >
        <div style={{ fontSize: '14px', lineHeight: '1.4' }}>
          {message.content.split('\n').map((line, index, array) => (
            <React.Fragment key={index}>
              {line}
              {index < array.length - 1 && <br />}
            </React.Fragment>
          ))}
        </div>
        <div
          style={{
            fontSize: '11px',
            opacity: 0.7,
            marginTop: '4px',
            textAlign: 'right',
          }}
        >
          {formatTimestamp(message.timestamp)}
        </div>
      </div>
    </div>
  );
};

export default ChatMessage;
