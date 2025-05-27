import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { Workout } from '../../models/workout.model';
import { UserSessionService } from '../../services/user-session.service';
import { WorkoutService } from '../../services/workout.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
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

  // Called when the button is clicked
  createWorkout() {
    // Prepare the workout object (user is required by the model, but backend may ignore it)
    const workout: Partial<Workout> = { date: this.getToday(), notes: '' };
    this.workoutService.createWorkout(workout as Workout).subscribe({
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
