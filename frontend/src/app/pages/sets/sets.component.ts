import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { WorkoutService } from '../../services/workout.service';

@Component({
  selector: 'app-sets',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    FormsModule,
    MatSnackBarModule,
  ],
  templateUrl: './sets.component.html',
  styleUrl: './sets.component.scss',
})
export class SetsComponent implements OnInit {
  // Store the ids passed from navigation state
  workoutId: string | undefined;
  exerciseId: string | undefined;
  workoutExerciseId: string | undefined;

  // Form values
  weight = 0;
  reps = 1;

  setsArray: { weight: number; reps: number }[] = [];
  exerciseName = '';

  constructor(
    private router: Router,
    private workoutService: WorkoutService,
    private snackBar: MatSnackBar,
  ) {
    // Get navigation state (safe for direct navigation)
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras.state as {
      workoutId?: string;
      exerciseId?: string;
      workoutExerciseId?: string;
      exerciseName?: string;
    };
    this.workoutId = state?.workoutId;
    this.exerciseId = state?.exerciseId;
    this.workoutExerciseId = state?.workoutExerciseId;
  }

  ngOnInit() {
    if (this.workoutExerciseId) {
      this.workoutService.getWorkoutExerciseById(this.workoutExerciseId).subscribe({
        next: data => {
          console.log('WorkoutExercise data:', data);
          this.exerciseName = data?.exercise?.name || '';
          if (data.sets && data.sets.trim() !== '') {
            try {
              this.setsArray = JSON.parse(data.sets);
            } catch (e) {
              console.error('Failed to parse sets:', e);
              this.setsArray = [];
            }
          } else {
            this.setsArray = [];
          }
        },
        error: err => console.error('Error fetching workoutExercise:', err),
      });
    }
  }

  // Increment/decrement handlers
  incrementWeight() {
    this.weight += 5;
  }
  decrementWeight() {
    if (this.weight >= 5) this.weight -= 5;
  }
  incrementReps() {
    this.reps += 1;
  }
  decrementReps() {
    if (this.reps > 1) this.reps -= 1;
  }

  // Save handler
  saveSet() {
    // Only proceed if workoutExerciseId exists
    if (!this.workoutExerciseId) return;

    // Push the new set to setsArray
    const newSet = { weight: this.weight, reps: this.reps };
    if (Array.isArray(this.setsArray)) {
      this.setsArray = [...this.setsArray, newSet];
    } else {
      this.setsArray = [newSet];
    }

    // Stringify and update via PUT
    this.workoutService.putWorkoutExerciseById(this.workoutExerciseId, this.setsArray).subscribe({
      next: data => {
        // Update setsArray with the response (parsed)
        if (data.sets && data.sets.trim() !== '') {
          try {
            this.setsArray = JSON.parse(data.sets);
          } catch (e) {
            console.error('Failed to parse sets after PUT:', e);
            // Keep local setsArray
          }
        }
      },
      error: err => console.error('Error updating workoutExercise:', err),
    });
  }

  /**
   * Deletes a set at the given index from setsArray and updates the backend.
   * @param index Index of the set to delete
   */
  deleteSet(index: number) {
    if (!this.workoutExerciseId) return;
    // Remove the set at the given index
    this.setsArray = this.setsArray.filter((_, i) => i !== index);
    // Update the backend with the new setsArray
    this.workoutService.putWorkoutExerciseById(this.workoutExerciseId, this.setsArray).subscribe({
      next: data => {
        // Update setsArray with the response (parsed)
        if (data.sets && data.sets.trim() !== '') {
          try {
            this.setsArray = JSON.parse(data.sets);
          } catch (e) {
            console.error('Failed to parse sets after DELETE:', e);
            // Keep local setsArray
          }
        }
      },
      error: err => console.error('Error updating workoutExercise after delete:', err),
    });
  }

  /**
   * Deletes the entire workout exercise and navigates home on success.
   * Shows a snackBar on error.
   */
  removeExercise() {
    if (!this.workoutExerciseId) return;
    this.workoutService.deleteWorkoutExerciseById(this.workoutExerciseId).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        this.snackBar.open('Failed to remove exercise. Please try again.', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
