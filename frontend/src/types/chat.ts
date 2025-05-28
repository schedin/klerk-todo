export enum MessageSender {
  USER = 'USER',
  ASSISTANT = 'ASSISTANT'
}

export interface ChatMessage {
  id: string;
  content: string;
  sender: MessageSender;
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
