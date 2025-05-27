import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Workout } from '../../models/workout.model';
import { UserSessionService } from '../../services/user-session.service';
import { WorkoutService } from '../../services/workout.service';

@Component({
  selector: 'app-workouts',
  imports: [CommonModule, RouterModule],
  templateUrl: './workouts.component.html',
  styleUrl: './workouts.component.scss',
})
export class WorkoutsComponent implements OnInit {
  workouts: Workout[] = [];

  constructor(
    private userSession: UserSessionService,
    private router: Router,
    private workoutService: WorkoutService,
  ) {}

  ngOnInit() {
    // If not logged in, redirect to home
    if (!this.userSession.isLoggedIn()) {
      this.router.navigate(['/']);
      return;
    }
    // Fetch all workouts
    this.workoutService.getAllWorkouts().subscribe({
      next: data => (this.workouts = data),
      error: err => console.error('Failed to load workouts', err),
    });
  }
}

// This is the workouts list page. The detail page is handled by WorkoutDetailComponent via the /workouts/:id route.
