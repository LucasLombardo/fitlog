export interface Exercise {
  id: string;
  muscleGroups: string;
  name: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
  createdBy: {
    id: string;
    email: string;
    password: string;
    createdAt: string;
    updatedAt: string;
    role: string;
  };
  public: boolean;
  active: boolean;
}
