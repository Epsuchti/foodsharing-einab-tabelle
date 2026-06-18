import { NgIf } from '@angular/common';
import { Component, inject } from '@angular/core';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { HelloService } from './api/api/hello.service';
import { HelloResponse } from './api/model/helloResponse';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NgIf],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly helloService = inject(HelloService);

  protected helloMessage = 'Loading...';
  protected timestamp = '';
  protected error = '';

  constructor() {
    this.loadHello();
  }

  private loadHello(): void {
    this.helloService.getHello()
      .pipe(
        catchError(() => {
          this.helloMessage = 'Backend unavailable';
          this.error = 'Could not reach the backend.';
          return of<HelloResponse | null>(null);
        })
      )
      .subscribe((response) => {
        if (!response) {
          this.timestamp = '';
          return;
        }

        this.helloMessage = response.message;
        this.timestamp = response.timestamp;
      });
  }
}
