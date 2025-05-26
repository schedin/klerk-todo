// Authentication service for the Todo app using real JWT tokens
import { jwtDecode } from 'jwt-decode';
import * as jose from 'jose';

// Define user roles/groups
export const userGroups: Record<string, string[]> = {
  'Alice': ['admins', 'users'],
  'Bob': ['users'],
  'Charlie': ['guests']
};

// Available groups
export const availableGroups = ['users', 'admins', 'guests'];

// Add a new user to userGroups
export const addUser = (username: string, groups: string[] = ['users']): void => {
  userGroups[username] = groups;
  // Save to localStorage for persistence
  saveUserGroupsToStorage();
};

// Delete a user from userGroups
export const deleteUser = (username: string): void => {
  if (userGroups[username]) {
    delete userGroups[username];
    // Save to localStorage for persistence
    saveUserGroupsToStorage();
  }
};

// Add a group to a user
export const addGroupToUser = (username: string, group: string): void => {
  if (!userGroups[username]) {
    userGroups[username] = [];
  }

  if (!userGroups[username].includes(group)) {
    userGroups[username].push(group);
    // Save to localStorage for persistence
    saveUserGroupsToStorage();
  }
};

// Remove a group from a user
export const removeGroupFromUser = (username: string, group: string): void => {
  if (userGroups[username]) {
    userGroups[username] = userGroups[username].filter(g => g !== group);
    // Save to localStorage for persistence
    saveUserGroupsToStorage();
  }
};

// Save userGroups to localStorage
const saveUserGroupsToStorage = (): void => {
  localStorage.setItem('userGroups', JSON.stringify(userGroups));
};

// Load userGroups from localStorage
export const loadUserGroupsFromStorage = (): void => {
  const storedGroups = localStorage.getItem('userGroups');
  if (storedGroups) {
    const parsedGroups = JSON.parse(storedGroups);
    // Merge with existing groups
    Object.keys(parsedGroups).forEach(username => {
      userGroups[username] = parsedGroups[username];
    });
  }
};

// JWT Secret key - should match the backend
const JWT_SECRET = "your-secret-key";
const JWT_ISSUER = "todo-app";
const JWT_AUDIENCE = "todo-app-users";

// User info interface
export interface UserInfo {
  username: string;
  groups: string[];
}

// JWT payload interface
interface JwtPayload {
  sub: string;        // subject (username)
  groups: string[];   // user groups
  iss: string;        // issuer
  aud: string;        // audience
  iat: number;        // issued at
  exp: number;        // expiration time
}

// Generate a real JWT token
export const generateToken = async (username: string): Promise<string> => {
  // Get user groups
  const groups = userGroups[username] || ['guest'];

  // Create JWT payload
  const payload = {
    sub: username,
    groups: groups,
    iss: JWT_ISSUER,
    aud: JWT_AUDIENCE,
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + (60 * 60 * 24) // 24 hours expiration
  };

  // Convert the secret to a Uint8Array
  const secretKey = new TextEncoder().encode(JWT_SECRET);

  // Sign the JWT with our secret key
  return await new jose.SignJWT(payload)
    .setProtectedHeader({ alg: 'HS256' })
    .sign(secretKey);
};

// Store the JWT token in localStorage
export const getAuthToken = (): string | null => {
  return localStorage.getItem('authToken');
};

export const setAuthToken = (token: string): void => {
  localStorage.setItem('authToken', token);
};

export const removeAuthToken = (): void => {
  localStorage.removeItem('authToken');
};

// Parse the JWT token to get user info
export const parseToken = (token: string): UserInfo | null => {
  try {
    // Decode the JWT (without verification in the browser)
    const decoded = jwtDecode<JwtPayload>(token);

    // Extract user info from the decoded token
    return {
      username: decoded.sub,
      groups: decoded.groups
    };
  } catch (error) {
    console.error('Error parsing JWT token:', error);
    return null;
  }
};

// Get the current user from the token
export const getCurrentUser = (): string | null => {
  const token = getAuthToken();
  if (!token) return null;

  const userInfo = parseToken(token);
  return userInfo?.username || null;
};

// Get user groups from the token
export const getUserGroups = (): string[] => {
  const token = getAuthToken();
  if (!token) return [];

  const userInfo = parseToken(token);
  return userInfo?.groups || [];
};

// Check if the current user is an admin
export const isAdmin = (): boolean => {
  const groups = getUserGroups();
  return groups.includes('admins');
};

// Initialize by loading from localStorage if available
try {
  loadUserGroupsFromStorage();
} catch (error) {
  console.error('Error loading user groups from storage:', error);
}
