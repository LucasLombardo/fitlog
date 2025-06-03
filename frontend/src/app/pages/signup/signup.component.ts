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

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss',
})
export class SignupComponent {
  email = '';
  password = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private userSession: UserSessionService,
    private snackBar: MatSnackBar,
  ) {}

  signup() {
    this.userSession.signup(this.email, this.password).subscribe({
      next: () => {
        // On successful signup, immediately log in
        this.userSession.login(this.email, this.password).subscribe({
          next: () => {
            this.router.navigate(['/']);
          },
          error: () => {
            this.snackBar.open(
              'Signup succeeded, but login failed. Please try logging in.',
              'Close',
              {
                duration: 3000,
              },
            );
          },
        });
      },
      error: err => {
        let msg = 'Signup failed. Please try again.';
        if (err && err.error && err.error.message) {
          msg = err.error.message;
        }
        this.snackBar.open(msg, 'Close', { duration: 3000 });
      },
    });
  }

  login() {
    this.router.navigate(['/login']);
  }
}
