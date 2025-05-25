import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { UsersListComponent } from '../../components/users-list/users-list.component';
import { UserSessionService } from '../../services/user-session.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule, UsersListComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  constructor(public userSession: UserSessionService) {}
}
