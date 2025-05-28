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
    // Log the id of the clicked WorkoutExercise
    console.log(ex.id);
  }

  addExercise() {
    if (this.workout?.id) {
      this.router.navigate(['/exercises'], {
        state: { workoutId: this.workout.id, fromHome: true },
      });
    }
  }
}
