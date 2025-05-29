import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { Exercise } from '../../models/exercise.model';
import { WorkoutExercise } from '../../models/workout.model';
import { ExercisesService } from '../../services/exercises.service';
import { WorkoutService } from '../../services/workout.service';
import { ExercisesComponent } from './exercises.component';

// Mock data
const mockExercises: Exercise[] = [
  {
    id: '1',
    name: 'Push-Up',
    muscleGroups: 'chest,triceps',
    notes: '',
    createdAt: '',
    updatedAt: '',
    createdBy: { id: '', email: '', password: '', createdAt: '', updatedAt: '', role: '' },
    public: true,
    active: true,
  },
  {
    id: '2',
    name: 'Squat',
    muscleGroups: 'legs,glutes',
    notes: '',
    createdAt: '',
    updatedAt: '',
    createdBy: { id: '', email: '', password: '', createdAt: '', updatedAt: '', role: '' },
    public: true,
    active: true,
  },
];

describe('ExercisesComponent', () => {
  let component: ExercisesComponent;
  let fixture: ComponentFixture<ExercisesComponent>;
  let exercisesServiceSpy: jasmine.SpyObj<ExercisesService>;
  let workoutServiceSpy: jasmine.SpyObj<WorkoutService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    exercisesServiceSpy = jasmine.createSpyObj('ExercisesService', ['getAllExercises']);
    workoutServiceSpy = jasmine.createSpyObj('WorkoutService', ['addWorkoutExercise']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'getCurrentNavigation']);
    routerSpy.getCurrentNavigation.and.returnValue({
      extras: { state: {} },
    } as unknown as ReturnType<Router['getCurrentNavigation']>);

    await TestBed.configureTestingModule({
      imports: [ExercisesComponent],
      providers: [
        { provide: ExercisesService, useValue: exercisesServiceSpy },
        { provide: WorkoutService, useValue: workoutServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExercisesComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load exercises on init', () => {
    exercisesServiceSpy.getAllExercises.and.returnValue(of(mockExercises));
    fixture.detectChanges();
    expect(component.exercises.length).toBe(2);
  });

  it('should filter by name (case and special char insensitive)', () => {
    component.exercises = mockExercises;
    component.filter = 'pushup';
    expect(component.filteredExercises.length).toBe(1);
    expect(component.filteredExercises[0].name).toBe('Push-Up');
  });

  it('should filter by muscle group (case and special char insensitive)', () => {
    component.exercises = mockExercises;
    component.filter = 'CHEST';
    expect(component.filteredExercises.length).toBe(1);
    expect(component.filteredExercises[0].name).toBe('Push-Up');
  });

  it('should return all exercises if filter is empty', () => {
    component.exercises = mockExercises;
    component.filter = '';
    expect(component.filteredExercises.length).toBe(2);
  });

  it('should return no exercises if filter does not match', () => {
    component.exercises = mockExercises;
    component.filter = 'xyz';
    expect(component.filteredExercises.length).toBe(0);
  });

  it('should call addWorkoutExercise and navigate', () => {
    component.workoutId = 'w1';
    workoutServiceSpy.addWorkoutExercise.and.returnValue(
      of({ id: 'mockWexId' } as WorkoutExercise),
    );
    component.fromHome = false;
    component.addWorkoutExercise('1');
    expect(workoutServiceSpy.addWorkoutExercise).toHaveBeenCalledWith('w1', '1');
    expect(routerSpy.navigate).toHaveBeenCalled();
  });

  it('should navigate to /exercises/new with workoutId in state when newExercise is called', () => {
    component.workoutId = 'test-id';
    component.newExercise();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/exercises/new'], {
      state: { workoutId: 'test-id' },
    });
  });
});
