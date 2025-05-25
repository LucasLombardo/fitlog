import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class UserSessionService {
  private user: { id: string; role: string; email: string } | null = null;
  private loggedIn = false;

  setUser(user: { id: string; role: string; email: string }) {
    this.user = user;
    this.loggedIn = true;
  }

  clearUser() {
    this.user = null;
    this.loggedIn = false;
  }

  getUser() {
    return this.user;
  }

  isLoggedIn() {
    return this.loggedIn;
  }
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  email = '';
  password = '';
  message = '';

  // TODO: Move this to a config file or environment variable
  private loginUrl = 'http://localhost:8080/users/login';

  constructor(private http: HttpClient, private router: Router, private userSession: UserSessionService) {}

  login() {
    this.http.post<any>(this.loginUrl, { email: this.email, password: this.password }).subscribe({
      next: (response) => {
        if (response && response.user) {
          this.userSession.setUser({
            id: response.user.id,
            role: response.user.role,
            email: response.user.email
          });
          this.router.navigate(['/']);
        } else {
          this.message = 'failure :('; // fallback if no user in response
        }
      },
      error: () => this.message = 'failure :('
    });
  }
}
