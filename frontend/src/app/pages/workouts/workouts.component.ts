import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserSessionService } from '../../services/user-session.service';

@Component({
  selector: 'app-workouts',
  imports: [],
  templateUrl: './workouts.component.html',
  styleUrl: './workouts.component.scss',
})
export class WorkoutsComponent implements OnInit {
  constructor(private userSession: UserSessionService, private router: Router) {}

  ngOnInit() {
    // If not logged in, redirect to home
    if (!this.userSession.isLoggedIn()) {
      this.router.navigate(['/']);
    }
  }
}
