import { Todo } from '../types/todo';
import { User } from '../types/user';
// Removed uuid import as it's not needed in the active application
// import { v4 as uuidv4 } from 'uuid';

// Initial mock data
let mockTodos: Todo[] = [
  {
    todoID: '1', // Replaced uuidv4() with static ID
    title: 'Learn React',
    description: 'Study React fundamentals and hooks',
    state: 'Created',
    createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString() // 7 days ago
  },
  {
    todoID: '2', // Replaced uuidv4() with static ID
    title: 'Build Todo App',
    description: 'Create a todo application with React and TypeScript',
    state: 'Created',
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString() // 3 days ago
  },
  {
    todoID: '3', // Replaced uuidv4() with static ID
    title: 'Learn Kotlin',
    description: 'Study Kotlin basics for backend development',
    state: 'Completed',
    createdAt: new Date(Date.now() - 10 * 24 * 60 * 60 * 1000).toISOString() // 10 days ago
  }
];

// Mock users data
const mockUsers: User[] = [
  { username: 'Alice' },
  { username: 'Bob' },
  { username: 'Charlie' }
];

// Mock users API
export const mockUserApi = {
  getAllUsers: async (): Promise<User[]> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([...mockUsers]);
      }, 300);
    });
  }
};

// Mock todos API functions
export const mockTodoApi = {
  getAllTodos: async (): Promise<Todo[]> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([...mockTodos]);
      }, 500);
    });
  },

  getTodoById: async (id: string): Promise<Todo | null> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const todo = mockTodos.find(todo => todo.todoID === id);
        resolve(todo || null);
      }, 300);
    });
  },

  createTodo: async (todoData: { title: string; description: string }): Promise<Todo> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        // Generate a simple ID based on timestamp
        const newTodo: Todo = {
          todoID: Date.now().toString(), // Replaced uuidv4() with timestamp-based ID
          title: todoData.title,
          description: todoData.description,
          state: 'Created',
          createdAt: new Date().toISOString()
        };
        mockTodos.push(newTodo);
        resolve(newTodo);
      }, 300);
    });
  },

  markComplete: async (id: string): Promise<Todo | null> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const todoIndex = mockTodos.findIndex(todo => todo.todoID === id);
        if (todoIndex !== -1) {
          mockTodos[todoIndex].state = 'Completed';
          resolve(mockTodos[todoIndex]);
        } else {
          resolve(null);
        }
      }, 300);
    });
  },

  markUncomplete: async (id: string): Promise<Todo | null> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const todoIndex = mockTodos.findIndex(todo => todo.todoID === id);
        if (todoIndex !== -1) {
          mockTodos[todoIndex].state = 'Created';
          resolve(mockTodos[todoIndex]);
        } else {
          resolve(null);
        }
      }, 300);
    });
  },

  moveToTrash: async (id: string): Promise<Todo | null> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const todoIndex = mockTodos.findIndex(todo => todo.todoID === id);
        if (todoIndex !== -1) {
          mockTodos[todoIndex].state = 'Trashed';
          resolve(mockTodos[todoIndex]);
        } else {
          resolve(null);
        }
      }, 300);
    });
  },

  deleteTodo: async (id: string): Promise<boolean> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const todoIndex = mockTodos.findIndex(todo => todo.todoID === id);
        if (todoIndex !== -1) {
          mockTodos.splice(todoIndex, 1);
          resolve(true);
        } else {
          resolve(false);
        }
      }, 300);
    });
  },

  untrashTodo: async (id: string): Promise<Todo | null> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const todoIndex = mockTodos.findIndex(todo => todo.todoID === id);
        if (todoIndex !== -1) {
          mockTodos[todoIndex].state = 'Created';
          resolve(mockTodos[todoIndex]);
        } else {
          resolve(null);
        }
      }, 300);
    });
  }
};
