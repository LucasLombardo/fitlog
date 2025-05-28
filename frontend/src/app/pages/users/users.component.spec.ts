import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { UserSessionService } from '../../services/user-session.service';

import { UsersComponent } from './users.component';

describe('UsersComponent', () => {
  let component: UsersComponent;
  let fixture: ComponentFixture<UsersComponent>;
  let userSessionSpy: jasmine.SpyObj<UserSessionService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    userSessionSpy = jasmine.createSpyObj('UserSessionService', ['isAdmin']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    await TestBed.configureTestingModule({
      imports: [UsersComponent],
      providers: [
        { provide: UserSessionService, useValue: userSessionSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(UsersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should redirect to home if not admin', () => {
    userSessionSpy.isAdmin.and.returnValue(false);
    component.ngOnInit();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should not redirect if admin', () => {
    userSessionSpy.isAdmin.and.returnValue(true);
    routerSpy.navigate.calls.reset();
    component.ngOnInit();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});
