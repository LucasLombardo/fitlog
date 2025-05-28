import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { Router } from '@angular/router';
import { Workout, WorkoutExercise } from '../../models/workout.model';

@Component({
  selector: 'app-workout',
  imports: [CommonModule, MatButtonModule, MatListModule, MatIconModule],
  templateUrl: './workout.component.html',
  styleUrl: './workout.component.scss',
})
export class WorkoutComponent {
  // Accept a Workout object from the parent component
  @Input() workout: Workout | null = null;

  constructor(private router: Router) {}

  // Getter to safely access exercises
  get exercises(): WorkoutExercise[] {
    return this.workout?.exercises ?? [];
  }

  onExerciseClick(ex: WorkoutExercise) {
    if (this.workout?.id && ex.exercise?.id && ex.id) {
      this.router.navigate(['/sets'], {
        state: {
          workoutId: this.workout.id,
          exerciseId: ex.exercise.id,
          workoutExerciseId: ex.id,
        },
      });
    }
  }

  addExercise() {
    if (this.workout?.id) {
      this.router.navigate(['/exercises'], {
        state: { workoutId: this.workout.id, fromHome: true },
      });
    }
  }

  /**
   * Parses the sets string from a WorkoutExercise and returns an array of set objects.
   * Returns an empty array if sets is missing or invalid.
   */
  parseSets(sets: string | undefined | null): { weight: number; reps: number }[] {
    if (!sets) return [];
    try {
      const parsed = JSON.parse(sets);
      if (Array.isArray(parsed)) {
        return parsed.filter(set => typeof set.weight === 'number' && typeof set.reps === 'number');
      }
    } catch {
      // If parsing fails, return empty array for safety
      return [];
    }
    return [];
  }

  /**
   * Returns true if the sets string is a valid, non-empty array.
   */
  hasSets(sets: string | undefined | null): boolean {
    return this.parseSets(sets).length > 0;
  }
}
