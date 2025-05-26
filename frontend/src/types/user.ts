export interface User {
  username: string;
  groups?: string[]; // Optional for backward compatibility
}
