import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { Workout } from '../../models/workout.model';
import { WorkoutService } from '../../services/workout.service';
import { WorkoutDetailComponent } from './workout-detail.component';

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

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('WorkoutService', ['getWorkoutById']);
    spy.getWorkoutById.and.returnValue(of(mockWorkout));
    await TestBed.configureTestingModule({
      imports: [WorkoutDetailComponent],
      providers: [
        { provide: WorkoutService, useValue: spy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } },
        },
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
});
