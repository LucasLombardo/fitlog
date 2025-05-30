import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Exercise } from '../models/exercise.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ExercisesService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllExercises(): Observable<Exercise[]> {
    return this.http.get<Exercise[]>(`${this.apiUrl}/exercises`, { withCredentials: true });
  }

  createExercise(exercise: {
    name: string;
    muscleGroups: string;
    notes: string;
  }): Observable<Exercise> {
    // Posts a new exercise to the backend. Returns the created Exercise object.
    return this.http.post<Exercise>(`${this.apiUrl}/exercises`, exercise, {
      withCredentials: true,
    });
  }
}
