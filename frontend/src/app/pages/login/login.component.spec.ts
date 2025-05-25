import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { UserSessionService } from './login.component';
import { of, throwError } from 'rxjs';

import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, HttpClientTestingModule, RouterTestingModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('UserSessionService', () => {
  let service: UserSessionService;
  beforeEach(() => {
    service = new UserSessionService();
  });
  it('should set and get user and login status', () => {
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getUser()).toBeNull();
    service.setUser({ id: '1', role: 'USER', email: 'a@b.com' });
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.getUser()).toEqual({ id: '1', role: 'USER', email: 'a@b.com' });
    service.clearUser();
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getUser()).toBeNull();
  });
});

describe('LoginComponent logic', () => {
  let component: LoginComponent;
  let userSession: UserSessionService;
  let http: any;
  let router: any;

  beforeEach(() => {
    userSession = new UserSessionService();
    http = { post: jasmine.createSpy() };
    router = { navigate: jasmine.createSpy() };
    component = new LoginComponent(http, router, userSession);
  });

  it('should store user info and navigate on successful login', () => {
    const user = { id: '1', role: 'USER', email: 'a@b.com' };
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
