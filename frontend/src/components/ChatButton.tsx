import React from 'react';

interface ChatButtonProps {
  onClick: () => void;
  isOpen: boolean;
}

const ChatButton: React.FC<ChatButtonProps> = ({ onClick, isOpen }) => {
  return (
    <button
      onClick={onClick}
      style={{
        position: 'fixed',
        bottom: '20px',
        right: '20px',
        width: '60px',
        height: '60px',
        borderRadius: '50%',
        backgroundColor: '#2196F3',
        color: 'white',
        border: 'none',
        cursor: 'pointer',
        fontSize: '24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxShadow: '0 4px 16px rgba(33, 150, 243, 0.3)',
        zIndex: 999,
        transition: 'all 0.3s ease',
        transform: isOpen ? 'rotate(45deg)' : 'rotate(0deg)',
      }}
      onMouseOver={(e) => {
        if (!isOpen) {
          e.currentTarget.style.transform = 'scale(1.1)';
          e.currentTarget.style.boxShadow = '0 6px 20px rgba(33, 150, 243, 0.4)';
        }
      }}
      onMouseOut={(e) => {
        if (!isOpen) {
          e.currentTarget.style.transform = 'scale(1)';
          e.currentTarget.style.boxShadow = '0 4px 16px rgba(33, 150, 243, 0.3)';
        }
      }}
    >
      {isOpen ? '✕' : '💬'}
    </button>
  );
};

export default ChatButton;
