import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { asyncScheduler } from 'rxjs';
import { observeOn } from 'rxjs/operators';
import { FooterComponent } from './layout/footer/footer.component';
import { LoadingService } from './core/services/loading.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, FooterComponent, MatProgressSpinnerModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  constructor(private readonly loadingService: LoadingService) {}

  get loading$() {
    return this.loadingService.loading$.pipe(observeOn(asyncScheduler));
  }
}