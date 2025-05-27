import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { UserRole } from '../models/user.model';
import { UserSessionService } from './user-session.service';

describe('UserSessionService', () => {
  let service: UserSessionService;
  beforeEach(() => {
    // Clear localStorage to avoid session persistence between tests
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [UserSessionService, provideHttpClient()],
    });
    service = TestBed.inject(UserSessionService);
  });
  it('should set and get user and login status', () => {
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getUser()).toBeNull();
    service.setUser({
      id: '1',
      role: UserRole.USER,
      email: 'a@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.getUser()).toEqual({
      id: '1',
      role: UserRole.USER,
      email: 'a@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    service.clearUser();
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getUser()).toBeNull();
  });
  it('should return true for admin and false for non-admin in isAdmin', () => {
    expect(service.isAdmin()).toBeFalse(); // no user
    service.setUser({
      id: '2',
      role: UserRole.USER,
      email: 'b@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    expect(service.isAdmin()).toBeFalse();
    service.setUser({
      id: '3',
      role: UserRole.ADMIN,
      email: 'admin@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    expect(service.isAdmin()).toBeTrue();
  });
});
