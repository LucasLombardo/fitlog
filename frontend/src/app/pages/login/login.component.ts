import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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
    this.userSession.login(this.email, this.password).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        this.message = 'failure :('; // fallback if login fails
      },
    });
  }
}
