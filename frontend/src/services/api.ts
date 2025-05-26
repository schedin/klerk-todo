// Using native fetch instead of axios to avoid Node.js dependencies
import { Todo, CreateTodoParams } from '../types/todo';
import { User } from '../types/user';
import { getAuthToken } from './auth';

const API_URL = '/api';  // Use the proxy configured in package.json

// Create a simple wrapper around fetch to avoid axios issues
const createApi = () => {
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
    get: async (url: string) => {
      const response = await fetch(`${API_URL}${url}`, {
        method: 'GET',
        headers: headers(),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }

      return { data: await response.json() };
    },

    post: async (url: string, data?: any) => {
      const response = await fetch(`${API_URL}${url}`, {
        method: 'POST',
        headers: headers(),
        body: data ? JSON.stringify(data) : undefined,
      });

      if (!response.ok) {
        const errorText = await response.text();
        const error = new Error(errorText);
        (error as any).response = { data: errorText };
        throw error;
      }

      return { data: await response.json() };
    },

    delete: async (url: string) => {
      const response = await fetch(`${API_URL}${url}`, {
        method: 'DELETE',
        headers: headers(),
      });

      if (!response.ok) {
        const errorText = await response.text();
        const error = new Error(errorText);
        (error as any).response = { data: errorText };
        throw error;
      }

      // For 204 No Content responses, don't try to parse JSON
      if (response.status === 204) {
        return { success: true };
      }

      // For other successful responses, parse JSON if there is content
      return { data: await response.json() };
    }
  };
};

const api = createApi();

// API functions for users
export const userApi = {
  // Get all users
  getAllUsers: async (): Promise<User[]> => {
    try {
      const response = await api.get('/users');

      // Import userGroups here to avoid circular dependency
      const { userGroups } = await import('./auth');

      // Add groups to each user
      return response.data.map((user: User) => ({
        ...user,
        groups: userGroups[user.username] || []
      }));
    } catch (error) {
      console.error('Error fetching users:', error);
      return [];
    }
  },

  // Delete a user
  deleteUser: async (username: string): Promise<boolean> => {
    try {
      await api.delete(`/users/${username}`);
      return true;
    } catch (error) {
      console.error(`Error deleting user ${username}:`, error);
      throw error; // Propagate the error to be handled by the caller
    }
  },
};

// API functions for todos
export const todoApi = {
  // Get all todos
  getAllTodos: async (): Promise<Todo[]> => {
    try {
      const response = await api.get('/todos');
      return response.data;
    } catch (error) {
      console.error('Error fetching todos:', error);
      return [];
    }
  },

  // Get a single todo by ID
  getTodoById: async (id: string): Promise<Todo | null> => {
    try {
      const response = await api.get(`/todos/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching todo with ID ${id}:`, error);
      return null;
    }
  },

  // Create a new todo
  createTodo: async (todoData: CreateTodoParams): Promise<Todo> => {
    try {
      const response = await api.post('/todos', todoData);
      return response.data;
    } catch (error: any) {
      console.error('Error creating todo:', error);
      // Propagate the error with the response data if available
      if (error.response && error.response.data) {
        throw new Error(error.response.data);
      }
      throw error; // Re-throw the original error if we can't extract a message
    }
  },

  // Mark a todo as complete
  markComplete: async (id: string): Promise<Todo> => {
    try {
      const response = await api.post(`/todos/${id}/complete`);
      return response.data;
    } catch (error: any) {
      console.error(`Error marking todo ${id} as complete:`, error);
      throw error; // Propagate the error to be handled by the caller
    }
  },

  // Mark a todo as uncomplete (revert from completed state)
  markUncomplete: async (id: string): Promise<Todo> => {
    try {
      const response = await api.post(`/todos/${id}/uncomplete`);
      return response.data;
    } catch (error: any) {
      console.error(`Error marking todo ${id} as uncomplete:`, error);
      throw error; // Propagate the error to be handled by the caller
    }
  },

  // Move a todo to trash
  moveToTrash: async (id: string): Promise<Todo> => {
    try {
      const response = await api.post(`/todos/${id}/trash`);
      return response.data;
    } catch (error: any) {
      console.error(`Error moving todo ${id} to trash:`, error);
      throw error; // Propagate the error to be handled by the caller
    }
  },

  // Delete a todo permanently
  deleteTodo: async (id: string): Promise<boolean> => {
    try {
      await api.delete(`/todos/${id}`);
      return true;
    } catch (error: any) {
      console.error(`Error deleting todo ${id}:`, error);
      throw error; // Propagate the error to be handled by the caller
    }
  },

  // Recover a todo from trash
  untrashTodo: async (id: string): Promise<Todo> => {
    try {
      const response = await api.post(`/todos/${id}/untrash`);
      return response.data;
    } catch (error: any) {
      console.error(`Error recovering todo ${id} from trash:`, error);
      throw error; // Propagate the error to be handled by the caller
    }
  },
};
