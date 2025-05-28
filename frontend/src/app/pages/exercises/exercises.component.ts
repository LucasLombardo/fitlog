import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { Router } from '@angular/router';
import { Exercise } from '../../models/exercise.model';
import { ExercisesService } from '../../services/exercises.service';
import { WorkoutService } from '../../services/workout.service';

@Component({
  selector: 'app-exercises',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatListModule,
    MatIconModule,
    MatInputModule,
    FormsModule,
  ],
  templateUrl: './exercises.component.html',
  styleUrl: './exercises.component.scss',
})
export class ExercisesComponent implements OnInit {
  workoutId: string | null = null;
  fromHome = false;
  exercises: Exercise[] = [];
  filter = '';

  constructor(
    private router: Router,
    private exercisesService: ExercisesService,
    private workoutService: WorkoutService,
  ) {
    // Access the navigation state to get the workoutId and fromHome
    const nav = this.router.getCurrentNavigation();
    this.workoutId = nav?.extras.state?.['workoutId'] || null;
    this.fromHome = nav?.extras.state?.['fromHome'] || false;
  }

  ngOnInit(): void {
    this.exercisesService.getAllExercises().subscribe({
      next: data => (this.exercises = data),
      error: err => console.error('Failed to load exercises', err),
    });
  }

  /**
   * Returns the list of exercises filtered by the filter string.
   * Matches substrings in name or muscleGroups, ignoring spaces and special characters.
   */
  get filteredExercises(): Exercise[] {
    if (!this.filter.trim()) return this.exercises;
    const normFilter = this.normalize(this.filter);
    return this.exercises.filter(
      ex =>
        this.normalize(ex.name).includes(normFilter) ||
        this.normalize(ex.muscleGroups).includes(normFilter),
    );
  }

  /**
   * Normalizes a string by removing spaces, special characters, and lowercasing.
   * @param str The string to normalize
   */
  private normalize(str: string): string {
    return str.toLowerCase().replace(/[^a-z0-9]/g, '');
  }

  addWorkoutExercise(exerciseId: string): void {
    console.log('Workout ID:', this.workoutId, 'Exercise ID:', exerciseId);
    this.workoutService.addWorkoutExercise(this.workoutId!, exerciseId).subscribe({
      next: () => {
        console.log('Workout exercise added');
        if (this.fromHome) {
          this.router.navigate([`/`]);
        } else {
          this.router.navigate([`/workouts/${this.workoutId}`]);
        }
      },
      error: err => console.error('Failed to add workout exercise', err),
    });
  }

  /**
   * Navigates to the new exercise creation page, passing the current workoutId as router state.
   */
  newExercise(): void {
    this.router.navigate(['/exercises/new'], { state: { workoutId: this.workoutId } });
  }
}
