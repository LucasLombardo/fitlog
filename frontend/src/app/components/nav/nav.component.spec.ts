import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { UserSessionService } from '../../services/user-session.service';
import { NavComponent } from './nav.component';

describe('NavComponent', () => {
  let component: NavComponent;
  let fixture: ComponentFixture<NavComponent>;
  let userSessionSpy: jasmine.SpyObj<UserSessionService>;

  beforeEach(async () => {
    userSessionSpy = jasmine.createSpyObj('UserSessionService', [
      'isLoggedIn',
      'isAdmin',
      'logout',
    ]);
    await TestBed.configureTestingModule({
      imports: [NavComponent],
      providers: [
        { provide: UserSessionService, useValue: userSessionSpy },
        provideRouter([]),
        provideHttpClient(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(NavComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render login and logout buttons based on login state', () => {
    userSessionSpy.isLoggedIn.and.returnValue(true);
    userSessionSpy.isAdmin.and.returnValue(false);
    fixture.detectChanges();
    const logoutBtn = fixture.debugElement.query(By.css('button'));
    expect(logoutBtn).toBeTruthy();
    userSessionSpy.isLoggedIn.and.returnValue(false);
    fixture.detectChanges();
    const loginLink = fixture.debugElement.nativeElement.textContent;
    expect(loginLink).toContain('Login');
  });
});
