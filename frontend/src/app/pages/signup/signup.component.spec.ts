import { HttpClient, provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter, Router, withDisabledInitialNavigation } from '@angular/router';
import { of, throwError } from 'rxjs';
import { UserSessionService } from '../../services/user-session.service';
import { SignupComponent } from './signup.component';

describe('SignupComponent', () => {
  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SignupComponent],
      providers: [provideRouter([], withDisabledInitialNavigation()), provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(SignupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('SignupComponent logic', () => {
  let component: SignupComponent;
  let userSession: UserSessionService;
  let router: Partial<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserSessionService, provideHttpClient()],
    });
    userSession = TestBed.inject(UserSessionService);
    spyOn(userSession, 'signup');
    spyOn(userSession, 'login');
    spyOn(userSession, 'verifyEmail');
    router = { navigate: jasmine.createSpy() };
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    component = new SignupComponent({} as HttpClient, router as Router, userSession, snackBar);
  });

  it('should call signup and login and navigate on success', () => {
    (userSession.signup as jasmine.Spy).and.returnValue(of({}));
    (userSession.login as jasmine.Spy).and.returnValue(of({}));
    component.email = 'a@b.com';
    component.password = 'pw';
    component.signup();
    expect(userSession.signup).toHaveBeenCalledWith('a@b.com', 'pw');
    expect(component.isVerifying).toBeTrue();
    (userSession.verifyEmail as jasmine.Spy).and.returnValue(of({ message: 'ok' }));
    (userSession.login as jasmine.Spy).and.returnValue(of({}));
    component.verificationCode = '123456';
    component.submitVerification();
    expect(userSession.verifyEmail).toHaveBeenCalledWith('a@b.com', '123456');
    expect(userSession.login).toHaveBeenCalledWith('a@b.com', 'pw');
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should show snackbar if signup fails', () => {
    (userSession.signup as jasmine.Spy).and.returnValue(throwError(() => new Error('fail')));
    component.signup();
    expect(snackBar.open).toHaveBeenCalled();
  });

  it('should show error if verification fails', () => {
    (userSession.signup as jasmine.Spy).and.returnValue(of({}));
    (userSession.verifyEmail as jasmine.Spy).and.returnValue(
      throwError(() => ({ error: { message: 'Invalid code' } })),
    );
    component.email = 'a@b.com';
    component.password = 'pw';
    component.signup();
    expect(component.isVerifying).toBeTrue();
    component.verificationCode = 'badcode';
    component.submitVerification();
    expect(component.verificationError).toBe('Invalid code');
  });
});
