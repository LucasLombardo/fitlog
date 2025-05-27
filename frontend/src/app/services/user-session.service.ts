import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { User, UserRole } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserSessionService {
  // Local state for user and login status
  private user: User | null = null;
  private loggedIn = false;

  // Key for localStorage
  private static readonly STORAGE_KEY = 'fitlog_user_session';
  // Session expiry in milliseconds (3 hours 30 minutes)
  private static readonly EXPIRY_MS = 3.5 * 60 * 60 * 1000;

  // Inject HttpClient for API calls
  private http = inject(HttpClient);

  constructor() {
    // On service init, try to restore session from localStorage
    this.restoreSession();
  }

  /**
   * Set the user in state and persist to localStorage with expiry.
   */
  setUser(user: User) {
    this.user = user;
    this.loggedIn = true;
    // Store user and expiry timestamp in localStorage
    const data = {
      user,
      expiresAt: Date.now() + UserSessionService.EXPIRY_MS,
    };
    localStorage.setItem(UserSessionService.STORAGE_KEY, JSON.stringify(data));
  }

  /**
   * Clear user from state and localStorage.
   */
  clearUser() {
    this.user = null;
    this.loggedIn = false;
    localStorage.removeItem(UserSessionService.STORAGE_KEY);
  }

  /**
   * Get the current user, or null if not logged in or expired.
   */
  getUser() {
    this.checkExpiry();
    return this.user;
  }

  /**
   * Returns true if user is logged in and session not expired.
   */
  isLoggedIn() {
    this.checkExpiry();
    return this.loggedIn;
  }

  /**
   * Returns true if user is admin and session not expired.
   */
  isAdmin() {
    this.checkExpiry();
    return this.user?.role === UserRole.ADMIN;
  }

  /**
   * Restore session from localStorage if not expired.
   */
  private restoreSession() {
    const dataStr = localStorage.getItem(UserSessionService.STORAGE_KEY);
    if (!dataStr) return;
    try {
      const data = JSON.parse(dataStr);
      if (data && data.user && data.expiresAt > Date.now()) {
        this.user = data.user;
        this.loggedIn = true;
      } else {
        // Expired or invalid
        this.clearUser();
      }
    } catch {
      this.clearUser();
    }
  }

  /**
   * Check if session is expired; if so, clear it.
   */
  private checkExpiry() {
    const dataStr = localStorage.getItem(UserSessionService.STORAGE_KEY);
    if (!dataStr) return;
    try {
      const data = JSON.parse(dataStr);
      if (!data.expiresAt || data.expiresAt <= Date.now()) {
        this.clearUser();
      }
    } catch {
      this.clearUser();
    }
  }

  /**
   * Logs out the user by POSTing to backend, clearing session, and returning observable.
   * Always clears session on client, even if backend fails.
   */
  logout(): Observable<unknown> {
    return this.http.post('http://localhost:8080/users/logout', {}, { withCredentials: true }).pipe(
      tap(() => this.clearUser()), // Clear session on success
      catchError(() => {
        this.clearUser(); // Clear session on error for security
        return of(null);
      }),
    );
  }

  /**
   * Logs in the user by POSTing credentials to backend, sets user on success, and returns observable.
   * Returns the user object on success, or throws on failure.
   */
  login(email: string, password: string): Observable<User> {
    interface LoginResponse {
      user?: User;
    }
    return this.http
      .post<LoginResponse>(
        'http://localhost:8080/users/login',
        { email, password },
        { withCredentials: true },
      )
      .pipe(
        map(response => {
          if (response && response.user) {
            const user: User = {
              id: response.user.id,
              role: response.user.role,
              email: response.user.email,
              updatedAt: response.user.updatedAt || new Date().toISOString(),
            };
            this.setUser(user);
            return user;
          } else {
            // Throw error as observable so type is always User
            throw new Error('Login failed: no user in response');
          }
        }),
      );
  }
}
