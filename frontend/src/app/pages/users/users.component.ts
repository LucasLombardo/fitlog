import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UsersListComponent } from '../../components/users-list/users-list.component';
import { UserSessionService } from '../../services/user-session.service';

@Component({
  selector: 'app-users',
  imports: [UsersListComponent],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent implements OnInit {
  constructor(
    private userSession: UserSessionService,
    private router: Router,
  ) {}

  ngOnInit() {
    // If not admin, redirect to home
    if (!this.userSession.isAdmin()) {
      this.router.navigate(['/']);
    }
  }
}
