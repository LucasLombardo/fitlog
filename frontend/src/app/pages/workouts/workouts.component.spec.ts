import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { Workout } from '../../models/workout.model';
import { UserSessionService } from '../../services/user-session.service';
import { WorkoutService } from '../../services/workout.service';
import { WorkoutsComponent } from './workouts.component';

const mockWorkouts: Workout[] = [
  {
    id: '1',
    user: { id: 'u1', email: '', password: '', createdAt: '', updatedAt: '', role: 'USER' },
    date: '2024-01-01',
    notes: '',
    createdAt: '',
    updatedAt: '',
  },
  {
    id: '2',
    user: { id: 'u2', email: '', password: '', createdAt: '', updatedAt: '', role: 'USER' },
    date: '2024-01-02',
    notes: '',
    createdAt: '',
    updatedAt: '',
  },
];

describe('WorkoutsComponent', () => {
  let component: WorkoutsComponent;
  let fixture: ComponentFixture<WorkoutsComponent>;
  let workoutServiceSpy: jasmine.SpyObj<WorkoutService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('WorkoutService', ['getAllWorkouts']);
    const userSessionSpy = jasmine.createSpyObj('UserSessionService', ['isLoggedIn']);
    userSessionSpy.isLoggedIn.and.returnValue(true);
    await TestBed.configureTestingModule({
      imports: [WorkoutsComponent, RouterTestingModule],
      providers: [
        { provide: WorkoutService, useValue: spy },
        { provide: UserSessionService, useValue: userSessionSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkoutsComponent);
    component = fixture.componentInstance;
    workoutServiceSpy = TestBed.inject(WorkoutService) as jasmine.SpyObj<WorkoutService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render a list of workout dates as links', () => {
    workoutServiceSpy.getAllWorkouts.and.returnValue(of(mockWorkouts));
    fixture.detectChanges();
    const links = fixture.debugElement.queryAll(By.css('a'));
    expect(links.length).toBe(2);
    expect(links[0].nativeElement.textContent).toContain('2024-01-01');
    expect(links[0].attributes['ng-reflect-router-link']).toContain('/workouts,1');
    expect(links[1].nativeElement.textContent).toContain('2024-01-02');
    expect(links[1].attributes['ng-reflect-router-link']).toContain('/workouts,2');
  });
});
