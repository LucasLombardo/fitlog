import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withDisabledInitialNavigation } from '@angular/router';
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
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let http: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let router: any;

  beforeEach(() => {
    userSession = new UserSessionService();
    http = { post: jasmine.createSpy() };
    router = { navigate: jasmine.createSpy() };
    component = new LoginComponent(http, router, userSession);
  });

  it('should store user info and navigate on successful login', () => {
    const user = { id: '1', role: UserRole.USER, email: 'a@b.com' };
    http.post.and.returnValue(of({ user }));
    component.email = 'a@b.com';
    component.password = 'pw';
    component.login();
    expect(userSession.isLoggedIn()).toBeTrue();
    expect(userSession.getUser()).toEqual(user);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should set failure message if login fails', () => {
    http.post.and.returnValue(throwError(() => new Error('fail')));
    component.login();
    expect(component.message).toContain('failure');
  });

  it('should set failure message if response has no user', () => {
    http.post.and.returnValue(of({}));
    component.login();
    expect(component.message).toContain('failure');
  });
});
