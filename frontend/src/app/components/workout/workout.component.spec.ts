import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { Workout } from '../../models/workout.model';
import { WorkoutComponent } from './workout.component';

describe('WorkoutComponent', () => {
  let component: WorkoutComponent;
  let fixture: ComponentFixture<WorkoutComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockWorkout: Workout = {
    id: 'w1',
    user: { id: 'u1', email: '', password: '', createdAt: '', updatedAt: '', role: 'USER' },
    date: '2024-01-01',
    notes: 'Test',
    createdAt: '',
    updatedAt: '',
    exercises: [
      {
        id: 'ex1',
        position: 0,
        notes: '',
        sets: '[{"weight":100,"reps":5}]',
        exercise: {
          id: 'e1',
          name: 'Bench',
          muscleGroups: 'chest',
          isPublic: true,
          isActive: true,
          notes: '',
        },
      },
    ],
  };

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    await TestBed.configureTestingModule({
      imports: [WorkoutComponent, HttpClientTestingModule],
      providers: [{ provide: Router, useValue: routerSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkoutComponent);
    component = fixture.componentInstance;
    component.workout = mockWorkout;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call addExercise and navigate', () => {
    component.addExercise();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/exercises'], {
      state: { workoutId: 'w1', fromHome: true },
    });
  });

  it('should call onExerciseClick and navigate', () => {
    const ex = mockWorkout.exercises![0];
    component.onExerciseClick(ex);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/sets'], {
      state: { workoutId: 'w1', exerciseId: 'e1', workoutExerciseId: 'ex1' },
    });
  });

  it('should parse sets correctly', () => {
    const sets = '[{"weight":100,"reps":5},{"weight":110,"reps":3}]';
    const parsed = component.parseSets(sets);
    expect(parsed.length).toBe(2);
    expect(parsed[0].weight).toBe(100);
    expect(parsed[1].reps).toBe(3);
  });

  it('should return empty array for invalid sets', () => {
    expect(component.parseSets('not-json')).toEqual([]);
    expect(component.parseSets(undefined)).toEqual([]);
    expect(component.parseSets(null)).toEqual([]);
  });

  it('should return true for hasSets if sets exist', () => {
    expect(component.hasSets('[{"weight":100,"reps":5}]')).toBeTrue();
    expect(component.hasSets('[]')).toBeFalse();
    expect(component.hasSets(undefined)).toBeFalse();
  });

  it('should render workout date and exercises in template', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('January 1, 2024');
    expect(compiled.textContent).toContain('Bench');
    expect(compiled.textContent).toContain('100lbs');
    expect(compiled.textContent).toContain('5 reps');
  });

  it('should call addExercise when Add Exercise button is clicked', () => {
    const button = fixture.debugElement.query(By.css('.add-exercise-button'));
    button.triggerEventHandler('click', null);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/exercises'], {
      state: { workoutId: 'w1', fromHome: true },
    });
  });

  it('should call onExerciseClick when exercise card is clicked', () => {
    const item = fixture.debugElement.query(By.css('.exercise-card'));
    item.triggerEventHandler('click', null);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/sets'], {
      state: { workoutId: 'w1', exerciseId: 'e1', workoutExerciseId: 'ex1' },
    });
  });
});
