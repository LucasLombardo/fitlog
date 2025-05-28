import { Component, Input } from '@angular/core';
import { Workout, WorkoutExercise } from '../../models/workout.model';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-workout',
  imports: [CommonModule, MatCardModule, MatButtonModule],
  templateUrl: './workout.component.html',
  styleUrl: './workout.component.scss'
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
      this.router.navigate(['/exercises'], { state: { workoutId: this.workout.id, fromHome: true } });
    }
  }
}
