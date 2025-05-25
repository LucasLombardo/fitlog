import { Injectable } from '@angular/core';
import { User, UserRole } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserSessionService {
  private user: User | null = null;
  private loggedIn = false;

  setUser(user: User) {
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

  isAdmin() {
    return this.user?.role === UserRole.ADMIN;
  }
}
