import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Workout } from '../../models/workout.model';
import { UserSessionService } from '../../services/user-session.service';
import { WorkoutService } from '../../services/workout.service';
import { WorkoutComponent } from '../../components/workout/workout.component';

@Component({
  selector: 'app-home',
  imports: [CommonModule, WorkoutComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  workout: Workout | null = null; // Store the created workout
  loading = false; // Track loading state

  constructor(
    public userSession: UserSessionService,
    private router: Router,
    private workoutService: WorkoutService,
  ) {}

  // Helper to get today's date in YYYY-MM-DD format
  getToday(): string {
    const today = new Date();
    return today.toISOString().slice(0, 10);
  }

  // Automatically create a workout if logged in
  ngOnInit() {
    if (this.userSession.isLoggedIn()) {
      this.createWorkout();
    }
  }

  // Create a workout and store it
  createWorkout() {
    this.loading = true;
    const workout: Partial<Workout> = { date: this.getToday(), notes: '' };
    this.workoutService.createWorkout(workout as Workout).subscribe({
      next: res => {
        this.workout = res;
        this.loading = false;
      },
      error: err => {
        // Handle error (could show a message)
        console.error('Failed to create workout', err);
        this.loading = false;
      },
    });
  }
}
