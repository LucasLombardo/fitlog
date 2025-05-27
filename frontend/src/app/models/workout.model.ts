export interface User {
  id: string;
  email: string;
  password: string;
  createdAt: string;
  updatedAt: string;
  role: string;
}

export interface WorkoutExercise {
  id: string;
  position: number;
  sets: string;
  notes: string;
  exercise: {
    id: string;
    name: string;
    muscleGroups: string;
    isPublic: boolean;
    isActive: boolean;
    notes: string;
  };
}

export interface Workout {
  id: string;
  user?: User;
  date: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
  exercises?: WorkoutExercise[] | null;
}
