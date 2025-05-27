import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Workout } from '../models/workout.model';

@Injectable({ providedIn: 'root' })
export class WorkoutService {
  private apiUrl = 'http://localhost:8080/workouts';

  constructor(private http: HttpClient) {}

  getWorkoutById(id: string): Observable<Workout> {
    // Calls GET http://localhost:8080/workouts/{id}
    return this.http.get<Workout>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  getAllWorkouts(): Observable<Workout[]> {
    // Calls GET http://localhost:8080/workouts
    return this.http.get<Workout[]>(this.apiUrl, { withCredentials: true });
  }
}
