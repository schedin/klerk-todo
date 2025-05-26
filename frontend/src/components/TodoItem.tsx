import React from 'react';
import { Todo } from '../types/todo';

interface TodoItemProps {
  todo: Todo;
  onComplete: (id: string) => void;
  onUncomplete: (id: string) => void;
  onTrash: (id: string) => void;
  onDelete: (id: string) => void;
  onUntrash: (id: string) => void;
  error?: string;
}

// Helper function to get color based on priority
const getPriorityColor = (priority: number): string => {
  switch (priority) {
    case 0: return '#e0e0e0'; // Light gray
    case 2: return '#c5e1a5'; // Light green
    case 4: return '#fff176'; // Light yellow
    case 6: return '#ffb74d'; // Light orange
    case 8: return '#ff8a65'; // Light red
    case 10: return '#e57373'; // Red
    default: return '#e0e0e0'; // Default light gray
  }
};

const TodoItem: React.FC<TodoItemProps> = ({ todo, onComplete, onUncomplete, onTrash, onDelete, onUntrash, error }) => {
  return (
    <div className="todo-item" style={{
      border: '1px solid #ddd',
      borderRadius: '4px',
      padding: '15px',
      marginBottom: '10px',
      backgroundColor: todo.state === 'Completed' ? '#f0fff0' : '#fff'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h3>{todo.title}</h3>
          <p>{todo.description}</p>
          <div style={{ fontSize: '0.8rem', color: '#666', marginTop: '5px' }}>
            {todo.createdAt && (
              <p style={{ margin: '0 0 3px 0' }}>
                Created: {new Date(todo.createdAt).toLocaleString()}
              </p>
            )}
            {todo.username && (
              <p style={{ margin: '0 0 3px 0' }}>
                Created by: {todo.username}
              </p>
            )}
            {todo.priority !== undefined && (
              <p style={{
                margin: '0',
                display: 'flex',
                alignItems: 'center'
              }}>
                <span>Priority: </span>
                <span style={{
                  display: 'inline-block',
                  marginLeft: '5px',
                  padding: '2px 6px',
                  borderRadius: '3px',
                  fontWeight: 'bold',
                  backgroundColor: getPriorityColor(todo.priority),
                  color: todo.priority > 6 ? 'white' : 'black'
                }}>
                  {todo.priority}
                </span>
              </p>
            )}
          </div>
        </div>
        {error && (
          <div style={{
            backgroundColor: '#ffebee',
            color: '#c62828',
            padding: '8px 12px',
            borderRadius: '4px',
            fontSize: '0.9rem',
            marginLeft: '10px',
            maxWidth: '250px',
            border: '1px solid #ef9a9a'
          }}>
            {error}
          </div>
        )}
      </div>
      <div className="todo-actions" style={{ marginTop: '10px', display: 'flex', gap: '10px' }}>
        {/* Only show action buttons for non-trashed todos */}
        {todo.state !== 'Trashed' ? (
          <>
            {todo.state !== 'Completed' && (
              <button
                onClick={() => onComplete(todo.todoID)}
                style={{
                  backgroundColor: '#4CAF50',
                  color: 'white',
                  border: 'none',
                  padding: '8px 12px',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Mark Complete
              </button>
            )}
            {todo.state === 'Completed' && (
              <button
                onClick={() => onUncomplete(todo.todoID)}
                style={{
                  backgroundColor: '#2196F3',
                  color: 'white',
                  border: 'none',
                  padding: '8px 12px',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                Mark Uncomplete
              </button>
            )}
            <button
              onClick={() => onTrash(todo.todoID)}
              style={{
                backgroundColor: '#f44336',
                color: 'white',
                border: 'none',
                padding: '8px 12px',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Move to Trash
            </button>
          </>
        ) : (
          /* For trashed todos, show Recover and Delete Permanently buttons */
          <>
            <button
              onClick={() => onUntrash(todo.todoID)}
              style={{
                backgroundColor: '#4CAF50',
                color: 'white',
                border: 'none',
                padding: '8px 12px',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Recover
            </button>
            <button
              onClick={() => onDelete(todo.todoID)}
              style={{
                backgroundColor: '#f44336',
                color: 'white',
                border: 'none',
                padding: '8px 12px',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Delete Permanently
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default TodoItem;
