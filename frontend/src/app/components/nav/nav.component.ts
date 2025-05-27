import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { UserSessionService } from '../../services/user-session.service';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './nav.component.html',
  styleUrl: './nav.component.scss',
})
export class NavComponent {
  private http = inject(HttpClient);
  private router = inject(Router);

  constructor(public userSession: UserSessionService) {}

  /**
   * Logs out the user by calling the user session service and routing home.
   */
  logout() {
    this.userSession.logout().subscribe(() => {
      this.router.navigate(['/']);
    });
  }
}
