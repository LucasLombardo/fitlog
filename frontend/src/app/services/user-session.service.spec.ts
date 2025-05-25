import { UserRole } from '../models/user.model';
import { UserSessionService } from './user-session.service';

describe('UserSessionService', () => {
  let service: UserSessionService;
  beforeEach(() => {
    service = new UserSessionService();
  });
  it('should set and get user and login status', () => {
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getUser()).toBeNull();
    service.setUser({ id: '1', role: UserRole.USER, email: 'a@b.com' });
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.getUser()).toEqual({ id: '1', role: UserRole.USER, email: 'a@b.com' });
    service.clearUser();
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getUser()).toBeNull();
  });
});
