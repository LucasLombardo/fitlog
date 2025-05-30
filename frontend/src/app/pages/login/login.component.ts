import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { UserSessionService } from '../../services/user-session.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  email = '';
  password = '';

  // Use the API base URL from the environment configuration
  private loginUrl = `${environment.apiUrl}/users/login`;

  constructor(
    private http: HttpClient,
    private router: Router,
    private userSession: UserSessionService,
    private snackBar: MatSnackBar,
  ) {}

  login() {
    this.userSession.login(this.email, this.password).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        // Show a snackbar for 3 seconds with a close option
        this.snackBar.open('Login failed: invalid credentials', 'Close', {
          duration: 3000,
        });
      },
    });
  }

  // Logs a message when the 'Forgot Password' button is clicked
  forgotPassword() {
    console.log('Forgot Password clicked');
  }

  // Logs a message when the 'Sign Up' button is clicked
  signUp() {
    console.log('Sign Up clicked');
  }
}
