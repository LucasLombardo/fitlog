import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { ExercisesService } from '../../services/exercises.service';
import { WorkoutService } from '../../services/workout.service';
import { NewExerciseComponent } from './new-exercise.component';

class MockExercisesService {}
class MockWorkoutService {}

describe('NewExerciseComponent', () => {
  let component: NewExerciseComponent;
  let fixture: ComponentFixture<NewExerciseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NewExerciseComponent,
        RouterTestingModule,
        HttpClientTestingModule,
        MatSnackBarModule,
      ],
      providers: [
        provideNoopAnimations(),
        { provide: ExercisesService, useClass: MockExercisesService },
        { provide: WorkoutService, useClass: MockWorkoutService },
        MatSnackBar,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(NewExerciseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('form should be invalid when empty', () => {
    expect(component.exerciseForm.valid).toBeFalsy();
  });

  it('form should be valid with name', () => {
    component.exerciseForm.controls['name'].setValue('Squat');
    expect(component.exerciseForm.valid).toBeTruthy();
  });
});
