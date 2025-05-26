import React from 'react';
import { Todo } from '../types/todo';
import TodoItem from './TodoItem';

interface TodoListProps {
  todos: Todo[];
  onComplete: (id: string) => void;
  onUncomplete: (id: string) => void;
  onTrash: (id: string) => void;
  onDelete: (id: string) => void;
  onUntrash: (id: string) => void;
  filter: string;
  todoErrors?: Record<string, string>;
}

const TodoList: React.FC<TodoListProps> = ({ todos, onComplete, onUncomplete, onTrash, onDelete, onUntrash, filter, todoErrors = {} }) => {
  // Filter todos based on the selected filter
  const filteredTodos = todos
    .filter(todo => {
      if (filter === 'all') return todo.state !== 'Trashed';
      if (filter === 'active') return todo.state === 'Created';
      if (filter === 'completed') return todo.state === 'Completed';
      if (filter === 'trashed') return todo.state === 'Trashed';
      return true;
    })
    // Sort todos by createdAt timestamp in descending order (newest first)
    .sort((a, b) => {
      // Handle cases where createdAt might be undefined
      if (!a.createdAt) return 1;
      if (!b.createdAt) return -1;
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });

  return (
    <div className="todo-list">
      <h2>
        {filter === 'all' && 'All Todos'}
        {filter === 'active' && 'Active Todos'}
        {filter === 'completed' && 'Completed Todos'}
        {filter === 'trashed' && 'Trashed Todos'}
      </h2>

      {filteredTodos.length === 0 ? (
        <p>No todos found.</p>
      ) : (
        filteredTodos.map(todo => (
          <TodoItem
            key={todo.todoID}
            todo={todo}
            onComplete={onComplete}
            onUncomplete={onUncomplete}
            onTrash={onTrash}
            onDelete={onDelete}
            onUntrash={onUntrash}
            error={todoErrors[todo.todoID]}
          />
        ))
      )}
    </div>
  );
};

export default TodoList;
