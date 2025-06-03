import { CommonModule, formatDate } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { environment } from '../../../environments/environment';
import { User } from '../../models/user.model';
import { UserSessionService } from '../../services/user-session.service';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './users-list.component.html',
  styleUrl: './users-list.component.scss',
})
export class UsersListComponent implements OnInit {
  users: User[] = [];
  loading = false;
  error = '';

  // Inject HttpClient and UserSessionService
  private http = inject(HttpClient);
  userSession = inject(UserSessionService);

  // Use the API base URL from the environment configuration
  private apiUrl = environment.apiUrl;

  ngOnInit() {
    // Only fetch users if not admin (per requirements)
    if (this.userSession.isAdmin()) {
      this.fetchUsers();
    }
  }

  fetchUsers() {
    this.loading = true;
    this.http.get<User[]>(`${this.apiUrl}/users`, { withCredentials: true }).subscribe({
      next: users => {
        this.users = users;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load users.';
        this.loading = false;
      },
    });
  }

  // Helper to format date as short string (e.g., 4/20/2025)
  formatShortDate(dateString: string): string {
    return formatDate(dateString, 'M/d/yyyy', 'en-US');
  }
}
