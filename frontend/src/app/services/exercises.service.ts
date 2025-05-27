import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Exercise } from '../models/exercise.model';

@Injectable({ providedIn: 'root' })
export class ExercisesService {
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getAllExercises(): Observable<Exercise[]> {
    return this.http.get<Exercise[]>(`${this.apiUrl}/exercises`, { withCredentials: true });
  }
}
