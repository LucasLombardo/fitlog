import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { User } from '../../models/user.model';
import { UserSessionService } from '../../services/user-session.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  email = '';
  password = '';
  message = '';

  // TODO: Move this to a config file or environment variable
  private loginUrl = 'http://localhost:8080/users/login';

  constructor(
    private http: HttpClient,
    private router: Router,
    private userSession: UserSessionService,
  ) {}

  login() {
    interface LoginResponse {
      user?: User;
    }
    this.http
      .post<LoginResponse>(
        this.loginUrl,
        { email: this.email, password: this.password },
        { withCredentials: true },
      )
      .subscribe({
        next: response => {
          if (response && response.user) {
            const user: User = {
              id: response.user.id,
              role: response.user.role,
              email: response.user.email,
              updatedAt: response.user.updatedAt || new Date().toISOString(),
            };
            this.userSession.setUser(user);
            this.router.navigate(['/']);
          } else {
            this.message = 'failure :('; // fallback if no user in response
          }
        },
        error: () => (this.message = 'failure :('),
      });
  }
}
