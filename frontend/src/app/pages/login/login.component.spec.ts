import { HttpClient, provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter, Router, withDisabledInitialNavigation } from '@angular/router';
import { of, throwError } from 'rxjs';
import { UserRole } from '../../models/user.model';
import { UserSessionService } from '../../services/user-session.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([], withDisabledInitialNavigation()), provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('LoginComponent logic', () => {
  let component: LoginComponent;
  let userSession: UserSessionService;
  let router: Partial<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserSessionService, provideHttpClient()],
    });
    userSession = TestBed.inject(UserSessionService);
    spyOn(userSession, 'login');
    router = { navigate: jasmine.createSpy() };
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    component = new LoginComponent({} as HttpClient, router as Router, userSession, snackBar);
  });

  it('should store user info and navigate on successful login', () => {
    const user = {
      id: '1',
      role: UserRole.USER,
      email: 'a@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    };
    (userSession.login as jasmine.Spy).and.returnValue(of(user));
    component.email = 'a@b.com';
    component.password = 'pw';
    component.login();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should show snackbar if login fails', () => {
    (userSession.login as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));
    component.login();
    expect(snackBar.open).toHaveBeenCalledWith('Login failed: invalid credentials', 'Close', {
      duration: 3000,
    });
  });
});
