export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
}

export interface User {
  id: string;
  role: UserRole;
  email: string;
  updatedAt: string; // ISO string from backend
}
