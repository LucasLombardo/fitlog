import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { WorkoutService } from '../../services/workout.service';

import { SetsComponent } from './sets.component';

describe('SetsComponent', () => {
  let component: SetsComponent;
  let fixture: ComponentFixture<SetsComponent>;
  let workoutServiceSpy: jasmine.SpyObj<WorkoutService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    workoutServiceSpy = jasmine.createSpyObj('WorkoutService', [
      'getWorkoutExerciseById',
      'putWorkoutExerciseById',
      'deleteWorkoutExerciseById',
    ]);
    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'getCurrentNavigation']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    // Always mock navigation state for all tests
    routerSpy.getCurrentNavigation.and.returnValue({
      extras: { state: { workoutExerciseId: 'ex1' } },
    } as unknown as ReturnType<Router['getCurrentNavigation']>);
    workoutServiceSpy.getWorkoutExerciseById.and.returnValue(
      of({
        id: 'ex1',
        position: 0,
        notes: '',
        sets: '',
        exercise: {},
      } as unknown as import('../../models/workout.model').WorkoutExercise),
    );

    await TestBed.configureTestingModule({
      imports: [SetsComponent],
      providers: [
        { provide: WorkoutService, useValue: workoutServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should increment and decrement weight and reps', () => {
    component.weight = 10;
    component.incrementWeight();
    expect(component.weight).toBe(15);
    component.decrementWeight();
    expect(component.weight).toBe(10);
    component.decrementWeight();
    expect(component.weight).toBe(5);
    component.decrementWeight();
    expect(component.weight).toBe(0);
    component.decrementWeight();
    expect(component.weight).toBe(0); // Should not go below 0

    component.reps = 1;
    component.incrementReps();
    expect(component.reps).toBe(2);
    component.decrementReps();
    expect(component.reps).toBe(1);
    component.decrementReps();
    expect(component.reps).toBe(1); // Should not go below 1
  });

  it('should save a set and reset input fields', () => {
    component.workoutExerciseId = 'ex1';
    workoutServiceSpy.putWorkoutExerciseById.and.returnValue(
      of({
        id: 'ex1',
        position: 0,
        notes: '',
        sets: '[{"weight":10,"reps":2}]',
        exercise: {},
      } as unknown as import('../../models/workout.model').WorkoutExercise),
    );
    component.weight = 10;
    component.reps = 2;
    component.saveSet();
    expect(workoutServiceSpy.putWorkoutExerciseById).toHaveBeenCalledWith('ex1', [
      { weight: 10, reps: 2 },
    ]);
    expect(component.weight).toBe(0);
    expect(component.reps).toBe(1);
    expect(component.setsArray).toEqual([{ weight: 10, reps: 2 }]);
  });

  it('should handle error on saveSet', () => {
    component.workoutExerciseId = 'ex1';
    workoutServiceSpy.putWorkoutExerciseById.and.returnValue(throwError(() => new Error('fail')));
    spyOn(console, 'error');
    component.saveSet();
    expect(console.error).toHaveBeenCalled();
  });

  it('should delete a set and update backend', () => {
    component.workoutExerciseId = 'ex1';
    component.setsArray = [
      { weight: 10, reps: 2 },
      { weight: 20, reps: 3 },
    ];
    workoutServiceSpy.putWorkoutExerciseById.and.returnValue(
      of({
        id: 'ex1',
        position: 0,
        notes: '',
        sets: '[{"weight":20,"reps":3}]',
        exercise: {},
      } as unknown as import('../../models/workout.model').WorkoutExercise),
    );
    component.deleteSet(0);
    expect(workoutServiceSpy.putWorkoutExerciseById).toHaveBeenCalledWith('ex1', [
      { weight: 20, reps: 3 },
    ]);
    expect(component.setsArray).toEqual([{ weight: 20, reps: 3 }]);
  });

  it('should handle error on deleteSet', () => {
    component.workoutExerciseId = 'ex1';
    component.setsArray = [{ weight: 10, reps: 2 }];
    workoutServiceSpy.putWorkoutExerciseById.and.returnValue(throwError(() => new Error('fail')));
    spyOn(console, 'error');
    component.deleteSet(0);
    expect(console.error).toHaveBeenCalled();
  });

  it('should remove exercise and navigate home', () => {
    component.workoutExerciseId = 'ex1';
    workoutServiceSpy.deleteWorkoutExerciseById.and.returnValue(of(undefined));
    component.removeExercise();
    expect(workoutServiceSpy.deleteWorkoutExerciseById).toHaveBeenCalledWith('ex1');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should navigate home on goHome()', () => {
    component.goHome();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });
});
