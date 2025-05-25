import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withDisabledInitialNavigation } from '@angular/router';
import { UserSessionService } from '../../services/user-session.service';
import { User, UserRole } from '../../models/user.model';
import { HomeComponent } from './home.component';

class MockUserSessionService {
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
}

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [provideRouter([], withDisabledInitialNavigation())],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('HomeComponent greeting', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let mockSession: MockUserSessionService;

  beforeEach(async () => {
    mockSession = new MockUserSessionService();
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [{ provide: UserSessionService, useValue: mockSession }],
    }).compileComponents();
    fixture = TestBed.createComponent(HomeComponent);
  });

  it('should show "Hello user" if not logged in', () => {
    mockSession.clearUser();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Hello user');
  });

  it('should show "Hello {email}" if logged in', () => {
    mockSession.setUser({ id: '1', role: UserRole.USER, email: 'a@b.com' });
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Hello a@b.com');
  });
});
