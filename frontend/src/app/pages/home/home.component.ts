import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { UserSessionService } from '../../services/user-session.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  constructor(public userSession: UserSessionService) {}
}
