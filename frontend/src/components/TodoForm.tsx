import React, { useState } from 'react';
import { CreateTodoParams } from '../types/todo';

interface TodoFormProps {
  onSubmit: (todo: CreateTodoParams) => Promise<boolean>;
}

const TodoForm: React.FC<TodoFormProps> = ({ onSubmit }) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<number>(0);

  // Calculate remaining characters for the title
  const maxTitleLength = 110;
  const remainingChars = maxTitleLength - title.length;
  const isNearLimit = remainingChars <= 20;
  const isAtLimit = remainingChars <= 0;

  // Calculate remaining characters for the description
  const maxDescriptionLength = 1000; // Set a reasonable limit
  const remainingDescChars = maxDescriptionLength - description.length;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const success = await onSubmit({ title, description, priority });
    if (success) {
      // Only clear the form on successful submission
      setTitle('');
      setDescription('');
      setPriority(0);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ marginBottom: '20px' }}>
      <div style={{ marginBottom: '20px' }}>
        <div style={{ position: 'relative' }}>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Todo title"
            maxLength={maxTitleLength}
            style={{
              width: '100%',
              padding: '8px',
              borderRadius: '4px',
              border: `1px solid ${isAtLimit ? '#f44336' : isNearLimit ? '#ff9800' : '#ccc'}`,
              backgroundColor: isAtLimit ? '#ffebee' : 'white'
            }}
          />
          <div
            style={{
              position: 'absolute',
              right: '8px',
              top: '8px',
              fontSize: '12px',
              color: isAtLimit ? '#f44336' : isNearLimit ? '#ff9800' : '#666',
              fontWeight: isNearLimit ? 'bold' : 'normal',
              backgroundColor: 'rgba(255, 255, 255, 0.8)',
              padding: '2px 6px',
              borderRadius: '3px',
              zIndex: 10
            }}
          >
            {remainingChars} characters remaining
          </div>
        </div>
      </div>
      <div style={{ marginBottom: '10px' }}>
        <div style={{ position: 'relative' }}>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description"
            maxLength={maxDescriptionLength}
            style={{
              width: '100%',
              padding: '8px',
              borderRadius: '4px',
              border: '1px solid #ccc',
              minHeight: '100px'
            }}
          />
          <div
            style={{
              position: 'absolute',
              right: '8px',
              bottom: '8px',
              fontSize: '12px',
              color: '#666',
              backgroundColor: 'rgba(255, 255, 255, 0.8)',
              padding: '2px 6px',
              borderRadius: '3px'
            }}
          >
            {remainingDescChars} characters remaining
          </div>
        </div>
      </div>
      <div style={{ marginBottom: '15px' }}>
        <label htmlFor="priority" style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
          Priority (0-10, even numbers only):
        </label>
        <select
          id="priority"
          value={priority}
          onChange={(e) => setPriority(Number(e.target.value))}
          style={{
            width: '250px',
            padding: '8px',
            borderRadius: '4px',
            border: '1px solid #ccc'
          }}
        >
          <option value="0">0 - Lowest</option>
          <option value="2">2 - Low</option>
          <option value="4">4 - Medium</option>
          <option value="6">6 - High</option>
          <option value="8">8 - Very High</option>
          <option value="10">10 - Highest</option>
        </select>
      </div>
      <button
        type="submit"
        disabled={isAtLimit}
        style={{
          backgroundColor: isAtLimit ? '#e0e0e0' : '#4CAF50',
          color: isAtLimit ? '#9e9e9e' : 'white',
          padding: '10px 20px',
          border: 'none',
          borderRadius: '4px',
          cursor: isAtLimit ? 'not-allowed' : 'pointer'
        }}
      >
        {isAtLimit ? 'Title Too Long' : 'Add Todo'}
      </button>
    </form>
  );
};

export default TodoForm;

