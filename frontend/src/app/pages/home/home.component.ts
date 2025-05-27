import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { UserSessionService } from '../../services/user-session.service';

// Interface for the workout response from backend
interface WorkoutResponse {
  updatedAt: string;
  createdAt: string;
  id: string;
  date: string;
  notes: string;
}

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  constructor(
    public userSession: UserSessionService,
    private http: HttpClient,
    private router: Router,
  ) {}

  // Helper to get today's date in YYYY-MM-DD format
  getToday(): string {
    const today = new Date();
    return today.toISOString().slice(0, 10);
  }

  // Called when the button is clicked
  createWorkout() {
    const body = { date: this.getToday(), notes: '' };
    this.http
      .post<WorkoutResponse>('http://localhost:8080/workouts', body, { withCredentials: true })
      .subscribe({
        next: res => {
          // Redirect to the new workout page using the returned id
          this.router.navigate(['/workouts', res.id]);
        },
        error: err => {
          // Handle error (could show a message)
          console.error('Failed to create workout', err);
        },
      });
  }
}
