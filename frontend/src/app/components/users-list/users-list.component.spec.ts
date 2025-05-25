import { HttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NEVER, of, throwError } from 'rxjs';
import { User, UserRole } from '../../models/user.model';
import { UserSessionService } from '../../services/user-session.service';

import { UsersListComponent } from './users-list.component';

// Mock UserSessionService
class MockUserSessionService {
  private admin = false;
  isAdmin() {
    return this.admin;
  }
  setAdmin(val: boolean) {
    this.admin = val;
  }
}

describe('UsersListComponent', () => {
  let component: UsersListComponent;
  let fixture: ComponentFixture<UsersListComponent>;
  let userSession: MockUserSessionService;
  let httpClientSpy: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    userSession = new MockUserSessionService();
    httpClientSpy = jasmine.createSpyObj('HttpClient', ['get']);
    await TestBed.configureTestingModule({
      imports: [UsersListComponent],
      providers: [
        { provide: UserSessionService, useValue: userSession },
        { provide: HttpClient, useValue: httpClientSpy },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(UsersListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not fetch users or show list if user is not admin', () => {
    userSession.setAdmin(false);
    spyOn(component, 'fetchUsers');
    component.ngOnInit();
    expect(component.fetchUsers).not.toHaveBeenCalled();
  });

  it('should fetch and display users if user is admin', () => {
    userSession.setAdmin(true);
    const users: User[] = [
      { id: '1', role: UserRole.USER, email: 'a@b.com', updatedAt: '2025-01-01T00:00:00.000Z' },
    ];
    httpClientSpy.get.and.returnValue(of(users));
    component.ngOnInit();
    expect(component.loading).toBeFalse();
    expect(component.users).toEqual(users);
  });

  it('should show error if fetch fails', () => {
    userSession.setAdmin(true);
    httpClientSpy.get.and.returnValue(throwError(() => new Error('fail')));
    component.ngOnInit();
    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Failed to load users.');
  });

  it('should show loading state while fetching', () => {
    userSession.setAdmin(true);
    // Use NEVER to simulate an observable that never emits
    httpClientSpy.get.and.returnValue(NEVER);
    component.fetchUsers();
    expect(component.loading).toBeTrue();
  });
});
