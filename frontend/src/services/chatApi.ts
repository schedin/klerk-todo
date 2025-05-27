import { ChatMessage, ChatMessageRequest, ChatMessageResponse, ChatHistoryResponse } from '../types/chat';
import { getAuthToken } from './auth';

const API_URL = '/api';

// Create a simple wrapper around fetch for chat API
const createChatApi = () => {
  const headers = () => {
    const baseHeaders = {
      'Content-Type': 'application/json',
    };

    const token = getAuthToken();
    if (token) {
      return {
        ...baseHeaders,
        'Authorization': `Bearer ${token}`
      };
    }

    return baseHeaders;
  };

  return {
    // Get chat history
    getHistory: async (): Promise<ChatMessage[]> => {
      const response = await fetch(`${API_URL}/chat/history`, {
        method: 'GET',
        headers: headers(),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }

      const data: ChatHistoryResponse = await response.json();
      return data.messages;
    },

    // Send a message
    sendMessage: async (content: string): Promise<ChatMessage> => {
      const request: ChatMessageRequest = { content };
      
      const response = await fetch(`${API_URL}/chat/message`, {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText);
      }

      const data: ChatMessageResponse = await response.json();
      return data.message;
    },

    // Clear chat history
    clearHistory: async (): Promise<void> => {
      const response = await fetch(`${API_URL}/chat/history`, {
        method: 'DELETE',
        headers: headers(),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText);
      }
    }
  };
};

export const chatApi = createChatApi();
