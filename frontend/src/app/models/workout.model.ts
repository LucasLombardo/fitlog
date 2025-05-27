export interface User {
  id: string;
  email: string;
  password: string;
  createdAt: string;
  updatedAt: string;
  role: string;
}

export interface Workout {
  id: string;
  user: User;
  date: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
}
