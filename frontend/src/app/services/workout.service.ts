import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Workout } from '../models/workout.model';

@Injectable({ providedIn: 'root' })
export class WorkoutService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  createWorkout(workout: Workout): Observable<Workout> {
    return this.http.post<Workout>(`${this.apiUrl}/workouts`, workout, { withCredentials: true });
  }

  getWorkoutById(id: string): Observable<Workout> {
    return this.http.get<Workout>(`${this.apiUrl}/workouts/${id}`, { withCredentials: true });
  }

  getAllWorkouts(): Observable<Workout[]> {
    return this.http.get<Workout[]>(`${this.apiUrl}/workouts`, { withCredentials: true });
  }

  putWorkoutById(id: string, workout: Workout): Observable<Workout> {
    return this.http.put<Workout>(`${this.apiUrl}/workouts/${id}`, workout, { withCredentials: true });
  }

  deleteWorkoutById(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/workouts/${id}`, { withCredentials: true });
  }

  addWorkoutExercise(workoutId: string, exerciseId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/workout_exercises`, { workoutId, exerciseId, sets: "", notes: "" }, { withCredentials: true });
  }
}
