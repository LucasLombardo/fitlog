import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { UserSessionService } from '../../services/user-session.service';

import { UsersComponent } from './users.component';

// Mock UserSessionService
class MockUserSessionService {
  isAdmin() { return false; }
  isLoggedIn() { return true; }
}

describe('UsersComponent', () => {
  let component: UsersComponent;
  let fixture: ComponentFixture<UsersComponent>;
  let userSession: MockUserSessionService;
  let httpClientSpy: jasmine.SpyObj<HttpClient>;

  beforeEach(async () => {
    userSession = new MockUserSessionService();
    httpClientSpy = jasmine.createSpyObj('HttpClient', ['get']);
    await TestBed.configureTestingModule({
      imports: [UsersComponent],
      providers: [
        { provide: UserSessionService, useValue: userSession },
        { provide: HttpClient, useValue: httpClientSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UsersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
