export interface ChatMessage {
  id: string;
  content: string;
  timestamp: number;
}

export interface ChatMessageRequest {
  content: string;
}

export interface ChatMessageResponse {
  message: ChatMessage;
}

export interface ChatHistoryResponse {
  messages: ChatMessage[];
}
