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
  verificationCode = '';
  isVerifying = false;
  signupDisabled = false;
  verificationError = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private userSession: UserSessionService,
    private snackBar: MatSnackBar,
  ) {}

  signup() {
    this.signupDisabled = true;
    this.userSession.signup(this.email, this.password).subscribe({
      next: () => {
        // On successful signup, prompt for verification code
        this.isVerifying = true;
        this.signupDisabled = true;
        this.verificationError = '';
      },
      error: err => {
        let msg = 'Signup failed. Please try again.';
        if (err && err.error && err.error.message) {
          msg = err.error.message;
        }
        this.snackBar.open(msg, 'Close', { duration: 3000 });
        this.signupDisabled = false;
      },
    });
  }

  submitVerification() {
    this.userSession.verifyEmail(this.email, this.verificationCode).subscribe({
      next: () => {
        // On successful verification, log in
        this.userSession.login(this.email, this.password).subscribe({
          next: () => {
            this.router.navigate(['/']);
          },
          error: () => {
            this.snackBar.open(
              'Verification succeeded, but login failed. Please try logging in.',
              'Close',
              { duration: 3000 },
            );
          },
        });
      },
      error: err => {
        let msg = 'Invalid verification code. Please try again.';
        if (err && err.error && err.error.message) {
          msg = err.error.message;
        }
        this.verificationError = msg;
      },
    });
  }

  login() {
    this.router.navigate(['/login']);
  }
}
