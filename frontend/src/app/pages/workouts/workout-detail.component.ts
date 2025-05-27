import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Workout } from '../../models/workout.model';
import { WorkoutService } from '../../services/workout.service';

@Component({
  selector: 'app-workout-detail',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  templateUrl: './workout-detail.component.html',
})
export class WorkoutDetailComponent {
  id: string | null = null;
  workout: Workout | null = null;

  constructor(
    private route: ActivatedRoute,
    private workoutService: WorkoutService,
    private router: Router,
  ) {
    // Get the id from the route parameters
    this.id = this.route.snapshot.paramMap.get('id');
    if (this.id) {
      this.workoutService.getWorkoutById(this.id).subscribe({
        next: data => (this.workout = data),
        error: err => console.error('Failed to load workout', err),
      });
    }
  }

  /**
   * Navigates to the /exercises page, passing the current workout id as state.
   */
  addExercise() {
    if (this.id) {
      this.router.navigate(['/exercises'], { state: { workoutId: this.id } });
    }
  }
}
