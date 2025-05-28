import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Exercise } from '../../models/exercise.model';
import { ExercisesService } from '../../services/exercises.service';
import { WorkoutService } from '../../services/workout.service';

@Component({
  selector: 'app-exercises',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './exercises.component.html',
})
export class ExercisesComponent implements OnInit {
  workoutId: string | null = null;
  fromHome = false;
  exercises: Exercise[] = [];

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
}
