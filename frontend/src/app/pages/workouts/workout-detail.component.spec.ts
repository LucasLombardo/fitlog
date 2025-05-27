import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { Workout } from '../../models/workout.model';
import { WorkoutService } from '../../services/workout.service';
import { WorkoutDetailComponent } from './workout-detail.component';
import { Router } from '@angular/router';

const mockWorkout: Workout = {
  id: '1',
  user: { id: 'u1', email: '', password: '', createdAt: '', updatedAt: '', role: 'USER' },
  date: '2024-01-01',
  notes: 'Test notes',
  createdAt: '',
  updatedAt: '',
};

describe('WorkoutDetailComponent', () => {
  let component: WorkoutDetailComponent;
  let fixture: ComponentFixture<WorkoutDetailComponent>;
  let workoutServiceSpy: jasmine.SpyObj<WorkoutService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('WorkoutService', ['getWorkoutById']);
    spy.getWorkoutById.and.returnValue(of(mockWorkout));
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    await TestBed.configureTestingModule({
      imports: [WorkoutDetailComponent],
      providers: [
        { provide: WorkoutService, useValue: spy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } },
        },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkoutDetailComponent);
    component = fixture.componentInstance;
    workoutServiceSpy = TestBed.inject(WorkoutService) as jasmine.SpyObj<WorkoutService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch and display workout date and notes', () => {
    workoutServiceSpy.getWorkoutById.and.returnValue(of(mockWorkout));
    fixture.detectChanges();
    const dateDiv = fixture.debugElement.query(By.css('div'));
    expect(dateDiv.nativeElement.textContent).toContain('2024-01-01');
    expect(dateDiv.nativeElement.textContent).toContain('Test notes');
  });

  it('should navigate to /exercises with workoutId in state when addExercise is called', () => {
    component.id = '123';
    component.addExercise();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/exercises'], { state: { workoutId: '123' } });
  });
});
