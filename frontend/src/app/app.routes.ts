import { Routes } from '@angular/router';
import { ExercisesComponent } from './pages/exercises/exercises.component';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { UsersComponent } from './pages/users/users.component';
import { WorkoutDetailComponent } from './pages/workouts/workout-detail.component';
import { WorkoutsComponent } from './pages/workouts/workouts.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'users', component: UsersComponent },
  { path: 'workouts', component: WorkoutsComponent },
  { path: 'workouts/:id', component: WorkoutDetailComponent },
  { path: 'exercises', component: ExercisesComponent },
];
