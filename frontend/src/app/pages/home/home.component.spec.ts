import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { UserRole } from '../../models/user.model';
import { UserSessionService } from '../../services/user-session.service';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let userSession: UserSessionService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent, HttpClientTestingModule, RouterTestingModule],
      providers: [UserSessionService],
    }).compileComponents();
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    userSession = TestBed.inject(UserSessionService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should show the create button if logged in', () => {
    userSession.setUser({
      id: '1',
      role: UserRole.USER,
      email: 'a@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    fixture.detectChanges();
    const button = fixture.debugElement.query(By.css('button'));
    expect(button).toBeTruthy();
  });

  it('should POST and navigate to workout detail on success', () => {
    spyOn(router, 'navigate');
    userSession.setUser({
      id: '1',
      role: UserRole.USER,
      email: 'a@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    fixture.detectChanges();
    component.createWorkout();
    const req = httpMock.expectOne('http://localhost:8080/workouts');
    expect(req.request.method).toBe('POST');
    req.flush({
      updatedAt: '2025-05-26T19:43:29.562175',
      createdAt: '2025-05-26T19:43:29.562175',
      id: 'test-id',
      date: '2025-05-26',
      notes: '',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/workouts', 'test-id']);
  });

  it('should handle error on failed POST', () => {
    spyOn(console, 'error');
    userSession.setUser({
      id: '1',
      role: UserRole.USER,
      email: 'a@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    fixture.detectChanges();
    component.createWorkout();
    const req = httpMock.expectOne('http://localhost:8080/workouts');
    req.flush('fail', { status: 500, statusText: 'Server Error' });
    expect(console.error).toHaveBeenCalled();
  });
});
