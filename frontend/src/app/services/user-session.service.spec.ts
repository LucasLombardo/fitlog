import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserRole } from '../models/user.model';
import { UserSessionService } from './user-session.service';

describe('UserSessionService', () => {
  let service: UserSessionService;
  let httpMock: HttpTestingController;
  beforeEach(() => {
    // Clear localStorage to avoid session persistence between tests
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [UserSessionService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserSessionService);
    httpMock = TestBed.inject(HttpTestingController);
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
  it('should verify email with code and return message', () => {
    service.verifyEmail('a@b.com', '123456').subscribe(res => {
      expect(res).toEqual({ message: 'Email verified successfully. You can now log in.' });
    });
    const req = httpMock.expectOne(r => r.url.includes('/users/verify-email'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.com', code: '123456' });
    req.flush({ message: 'Email verified successfully. You can now log in.' });
    httpMock.verify();
  });
});
