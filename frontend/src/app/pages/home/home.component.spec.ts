import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
// import { ComponentFixture } from '@angular/core/testing'; // Removed unused import
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { environment } from '../../../environments/environment';
import { UserRole } from '../../models/user.model';
import { UserSessionService } from '../../services/user-session.service';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  let userSession: UserSessionService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent, HttpClientTestingModule, RouterTestingModule],
      providers: [UserSessionService],
    }).compileComponents();
    userSession = TestBed.inject(UserSessionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should automatically create and display a workout if logged in', () => {
    userSession.setUser({
      id: '1',
      role: UserRole.USER,
      email: 'a@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    const fixture = TestBed.createComponent(HomeComponent);
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/workouts`);
    expect(req.request.method).toBe('POST');
    req.flush({
      updatedAt: '2025-05-26T19:43:29.562175',
      createdAt: '2025-05-26T19:43:29.562175',
      id: 'test-id',
      date: '2025-05-26',
      notes: '',
    });
    fixture.detectChanges();
    // Check that the workout is displayed in the template
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('May 26, 2025');
    expect(compiled.textContent).toContain('Add Exercise');
  });

  it('should handle error on failed POST', () => {
    spyOn(console, 'error');
    userSession.setUser({
      id: '1',
      role: UserRole.USER,
      email: 'a@b.com',
      updatedAt: '2025-01-01T00:00:00.000Z',
    });
    const fixture = TestBed.createComponent(HomeComponent);
    fixture.detectChanges();
    const req = httpMock.expectOne(`${environment.apiUrl}/workouts`);
    req.flush('fail', { status: 500, statusText: 'Server Error' });
    expect(console.error).toHaveBeenCalled();
  });
});
