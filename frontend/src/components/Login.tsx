import React, { useState, useEffect } from 'react';
import { User } from '../types/user';
import { userApi } from '../services/api';
import {
  generateToken,
  setAuthToken,
  addUser,
  deleteUser as deleteUserFromGroups,
  addGroupToUser,
  removeGroupFromUser,
  availableGroups,
  loadUserGroupsFromStorage
} from '../services/auth';

interface LoginProps {
  onLogin: (username: string) => void;
}

const Login: React.FC<LoginProps> = ({ onLogin }) => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [loginInProgress, setLoginInProgress] = useState<string | null>(null);
  const [showUserManager, setShowUserManager] = useState<boolean>(false);
  const [newUsername, setNewUsername] = useState<string>('');
  const [deleteInProgress, setDeleteInProgress] = useState<string | null>(null);
  const [addGroupDropdown, setAddGroupDropdown] = useState<string | null>(null);

  useEffect(() => {
    // Load user groups from localStorage
    loadUserGroupsFromStorage();

    const fetchUsers = async () => {
      setLoading(true);
      try {
        const data = await userApi.getAllUsers();
        setUsers(data);
        setError(null);
      } catch (err) {
        setError('Failed to fetch users. Please try again later.');
        console.error('Error fetching users:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, []);

  const handleLogin = async (username: string) => {
    setLoginInProgress(username);
    setError(null);

    try {
      // Generate JWT token asynchronously
      const token = await generateToken(username);
      setAuthToken(token);
      onLogin(username);
    } catch (error) {
      console.error('Error generating token:', error);
      setError('Failed to login. Please try again.');
    } finally {
      setLoginInProgress(null);
    }
  };

  // Toggle user manager visibility
  const toggleUserManager = () => {
    setShowUserManager(prev => !prev);
  };

  // Handle adding a new user
  const handleAddUser = () => {
    if (!newUsername || newUsername.trim().length < 3) {
      setError('Username must be at least 3 characters long');
      return;
    }

    // Check if username already exists
    if (users.some(user => user.username === newUsername)) {
      setError('Username already exists');
      return;
    }

    // Add user to local state and storage
    addUser(newUsername);

    // Add user to the list
    setUsers([...users, { username: newUsername, groups: ['users'] }]);

    // Clear input
    setNewUsername('');
    setError(null);
  };

  // Handle deleting a user
  const handleDeleteUser = async (username: string) => {
    setDeleteInProgress(username);
    setError(null);

    try {
      // Delete from backend
      await userApi.deleteUser(username);

      // Delete from local storage
      deleteUserFromGroups(username);

      // Remove from UI
      setUsers(users.filter(user => user.username !== username));
    } catch (err) {
      setError(`Failed to delete user ${username}. Please try again.`);
      console.error('Error deleting user:', err);
    } finally {
      setDeleteInProgress(null);
    }
  };

  // Handle adding a group to a user
  const handleAddGroup = (username: string, group: string) => {
    // Add group to user
    addGroupToUser(username, group);

    // Update UI
    setUsers(users.map(user => {
      if (user.username === username) {
        // Create a new array with the group added
        const updatedGroups = [...(user.groups || [])];
        if (!updatedGroups.includes(group)) {
          updatedGroups.push(group);
        }
        return { ...user, groups: updatedGroups };
      }
      return user;
    }));

    // Close dropdown
    setAddGroupDropdown(null);
  };

  // Handle removing a group from a user
  const handleRemoveGroup = (username: string, group: string) => {
    // Remove group from user
    removeGroupFromUser(username, group);

    // Update UI
    setUsers(users.map(user =>
      user.username === username
        ? { ...user, groups: (user.groups || []).filter(g => g !== group) }
        : user
    ));
  };

  return (
    <div className="login-container" style={{
      maxWidth: '500px',
      margin: '100px auto',
      padding: '20px',
      borderRadius: '8px',
      boxShadow: '0 4px 8px rgba(0, 0, 0, 0.1)',
      backgroundColor: 'white'
    }}>
      <h2 style={{ textAlign: 'center', marginBottom: '20px' }}>Login to Todo App</h2>

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '10px' }}>
        <button
          onClick={toggleUserManager}
          style={{
            padding: '6px 12px',
            backgroundColor: showUserManager ? '#f44336' : '#2196F3',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '14px'
          }}
        >
          {showUserManager ? 'Close User Manager' : 'Manage Users'}
        </button>
      </div>

      {error && (
        <div style={{
          backgroundColor: '#ffebee',
          color: '#c62828',
          padding: '10px',
          borderRadius: '4px',
          marginBottom: '20px'
        }}>
          {error}
        </div>
      )}

      {loading ? (
        <p style={{ textAlign: 'center' }}>Loading users...</p>
      ) : showUserManager ? (
        <div className="user-manager" style={{ marginBottom: '20px' }}>
          <h3 style={{ marginBottom: '15px' }}>User Manager</h3>

          {/* Add new user form */}
          <div style={{
            display: 'flex',
            marginBottom: '20px',
            gap: '10px'
          }}>
            <input
              type="text"
              value={newUsername}
              onChange={(e) => setNewUsername(e.target.value)}
              placeholder="New username (min 3 chars)"
              style={{
                padding: '8px 12px',
                borderRadius: '4px',
                border: '1px solid #ccc',
                flex: 1
              }}
            />
            <button
              onClick={handleAddUser}
              style={{
                padding: '8px 12px',
                backgroundColor: '#4caf50',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Add User
            </button>
          </div>

          {/* User list with management options */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {users.map(user => (
              <div
                key={user.username}
                style={{
                  padding: '12px',
                  backgroundColor: '#f5f5f5',
                  borderRadius: '4px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '10px'
                }}
              >
                <div style={{ fontWeight: 'bold' }}>{user.username}</div>

                <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
                  {/* Group badges with remove option */}
                  <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                    {user.groups?.map((group: string, index: number) => (
                      <div
                        key={index}
                        style={{
                          backgroundColor: group === 'admins' ? '#ff9800' :
                                          group === 'users' ? '#4caf50' : '#9e9e9e',
                          color: 'white',
                          padding: '2px 6px',
                          borderRadius: '10px',
                          fontSize: '12px',
                          fontWeight: 'bold',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '4px'
                        }}
                      >
                        {group}
                        <span
                          onClick={() => handleRemoveGroup(user.username, group)}
                          style={{
                            cursor: 'pointer',
                            fontWeight: 'bold',
                            fontSize: '14px'
                          }}
                        >
                          ×
                        </span>
                      </div>
                    ))}
                  </div>

                  {/* Add group dropdown */}
                  <div style={{ position: 'relative' }}>
                    <button
                      onClick={() => setAddGroupDropdown(addGroupDropdown === user.username ? null : user.username)}
                      style={{
                        padding: '2px 6px',
                        backgroundColor: '#2196F3',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '12px'
                      }}
                    >
                      +
                    </button>

                    {addGroupDropdown === user.username && (
                      <div style={{
                        position: 'absolute',
                        top: '100%',
                        right: 0,
                        backgroundColor: 'white',
                        boxShadow: '0 2px 5px rgba(0,0,0,0.2)',
                        borderRadius: '4px',
                        zIndex: 10,
                        minWidth: '100px'
                      }}>
                        {availableGroups
                          .filter(group => !user.groups?.includes(group))
                          .map(group => (
                            <div
                              key={group}
                              onClick={() => handleAddGroup(user.username, group)}
                              style={{
                                padding: '8px 12px',
                                cursor: 'pointer',
                                borderBottom: '1px solid #eee'
                              }}
                              onMouseOver={(e) => {
                                e.currentTarget.style.backgroundColor = '#f5f5f5';
                              }}
                              onMouseOut={(e) => {
                                e.currentTarget.style.backgroundColor = 'white';
                              }}
                            >
                              {group}
                            </div>
                          ))}
                      </div>
                    )}
                  </div>

                  {/* Delete user button */}
                  <button
                    onClick={() => handleDeleteUser(user.username)}
                    disabled={deleteInProgress !== null}
                    style={{
                      padding: '2px 6px',
                      backgroundColor: deleteInProgress === user.username ? '#ffcdd2' : '#f44336',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: deleteInProgress !== null ? 'default' : 'pointer',
                      fontSize: '12px'
                    }}
                  >
                    {deleteInProgress === user.username ? 'Deleting...' : 'Delete'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div>
          <p style={{ marginBottom: '15px', textAlign: 'center' }}>Select a user to continue:</p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {users.map(user => (
              <button
                key={user.username}
                onClick={() => handleLogin(user.username)}
                disabled={loginInProgress !== null}
                style={{
                  padding: '12px',
                  backgroundColor: loginInProgress === user.username ? '#BBDEFB' : '#2196F3',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: loginInProgress !== null ? 'default' : 'pointer',
                  fontSize: '16px',
                  transition: 'background-color 0.3s',
                  position: 'relative',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}
                onMouseOver={(e) => {
                  if (loginInProgress === null) {
                    e.currentTarget.style.backgroundColor = '#1976D2';
                  }
                }}
                onMouseOut={(e) => {
                  if (loginInProgress === null) {
                    e.currentTarget.style.backgroundColor = '#2196F3';
                  }
                }}
              >
                <span>{loginInProgress === user.username ? 'Logging in...' : user.username}</span>
                <div style={{ display: 'flex', gap: '4px' }}>
                  {user.groups?.map((group: string, index: number) => (
                    <span
                      key={index}
                      style={{
                        backgroundColor: group === 'admins' ? '#ff9800' :
                                        group === 'users' ? '#4caf50' : '#9e9e9e',
                        color: 'white',
                        padding: '2px 6px',
                        borderRadius: '10px',
                        fontSize: '12px',
                        fontWeight: 'bold'
                      }}
                    >
                      {group}
                    </span>
                  ))}
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default Login;
