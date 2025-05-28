import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ExercisesService } from '../../services/exercises.service';
import { WorkoutService } from '../../services/workout.service';
import { firstValueFrom } from 'rxjs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-new-exercise',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './new-exercise.component.html',
  styleUrl: './new-exercise.component.scss',
})
export class NewExerciseComponent {
  exerciseForm: FormGroup;
  workoutId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private exercisesService: ExercisesService,
    private workoutService: WorkoutService,
    private snackBar: MatSnackBar
  ) {
    // Get workoutId from router state
    const nav = this.router.getCurrentNavigation();
    this.workoutId = nav?.extras.state?.['workoutId'] || null;
    // Initialize the form
    this.exerciseForm = this.fb.group({
      name: ['', Validators.required],
      muscleGroups: [''],
      notes: ['']
    });
  }

  // Handle form submission
  async createExercise(): Promise<void> {
    if (!this.exerciseForm.valid) return;
    if (!this.workoutId) return;
    const { name, muscleGroups, notes } = this.exerciseForm.value;
    try {
      // 1. Create the exercise
      const exercise = await firstValueFrom(
        this.exercisesService.createExercise({ name, muscleGroups, notes })
      );
      // 2. Add the exercise to the workout
      await firstValueFrom(
        this.workoutService.addWorkoutExercise(this.workoutId, exercise.id)
      );
      // 3. Navigate to home on success
      this.router.navigate(['/']);
    } catch (error) {
      // Show a user-friendly error message
      this.snackBar.open('Failed to create exercise or add to workout', 'Close', {
        duration: 3000,
      });
      console.error('Failed to create exercise or add to workout:', error);
    }
  }
} 