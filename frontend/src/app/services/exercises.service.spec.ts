import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Exercise } from '../models/exercise.model';
import { ExercisesService } from './exercises.service';
import { environment } from '../../environments/environment';

const mockExercises: Exercise[] = [
  {
    id: '1',
    muscleGroups: 'Chest',
    name: 'Bench Press',
    notes: 'Use spotter',
    createdAt: '',
    updatedAt: '',
    createdBy: {
      id: 'u1',
      email: 'test@example.com',
      password: 'secret',
      createdAt: '',
      updatedAt: '',
      role: 'USER',
    },
    public: true,
    active: true,
  },
];

describe('ExercisesService', () => {
  let service: ExercisesService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ExercisesService],
    });
    service = TestBed.inject(ExercisesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch all exercises', () => {
    service.getAllExercises().subscribe(exercises => {
      expect(exercises).toEqual(mockExercises);
    });
    const req = httpMock.expectOne(`${environment.apiUrl}/exercises`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockExercises);
  });
});
