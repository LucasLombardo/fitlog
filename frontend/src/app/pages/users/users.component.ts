import { Component } from '@angular/core';
import { UsersListComponent } from '../../components/users-list/users-list.component';

@Component({
  selector: 'app-users',
  imports: [UsersListComponent],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent {}
