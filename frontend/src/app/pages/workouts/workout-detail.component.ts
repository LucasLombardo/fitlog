import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-workout-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './workout-detail.component.html',
})
export class WorkoutDetailComponent {
  id: string | null = null;

  constructor(private route: ActivatedRoute) {
    // Get the id from the route parameters
    this.id = this.route.snapshot.paramMap.get('id');
  }
}
