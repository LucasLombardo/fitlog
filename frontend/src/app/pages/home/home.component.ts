import { Component } from '@angular/core';
import { UserSessionService } from '../login/login.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  constructor(public userSession: UserSessionService) {}
}
