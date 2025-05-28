import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Workout } from '../models/workout.model';
import { WorkoutService } from './workout.service';
import { WorkoutExercise } from '../models/workout.model';

const mockWorkout: Workout = {
  id: '1',
  user: {
    id: 'u1',
    email: 'test@example.com',
    password: 'secret',
    createdAt: '',
    updatedAt: '',
    role: 'USER',
  },
  date: '2024-01-01',
  notes: 'Test notes',
  createdAt: '',
  updatedAt: '',
};

describe('WorkoutService', () => {
  let service: WorkoutService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WorkoutService],
    });
    service = TestBed.inject(WorkoutService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch a workout by id', () => {
    service.getWorkoutById('1').subscribe(workout => {
      expect(workout).toEqual(mockWorkout);
    });
    const req = httpMock.expectOne('http://localhost:8080/workouts/1');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockWorkout);
  });

  it('should fetch all workouts', () => {
    service.getAllWorkouts().subscribe(workouts => {
      expect(workouts).toEqual([mockWorkout]);
    });
    const req = httpMock.expectOne('http://localhost:8080/workouts');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush([mockWorkout]);
  });

  it('should add an exercise to a workout', () => {
    const mockWorkoutExercise: WorkoutExercise = {
      id: 'mockWexId',
      position: 1,
      sets: '',
      notes: '',
      exercise: {
        id: '2',
        name: 'Test Exercise',
        muscleGroups: '',
        isPublic: true,
        isActive: true,
        notes: '',
      },
    };
    service.addWorkoutExercise('1', '2').subscribe(response => {
      expect(response).toEqual(jasmine.objectContaining({ id: 'mockWexId', position: 1 }));
    });
    const req = httpMock.expectOne('http://localhost:8080/workout_exercises');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ workoutId: '1', exerciseId: '2', sets: '', notes: '', position: 1 });
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockWorkoutExercise);
  });
});
