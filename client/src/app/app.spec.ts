import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { provideApi } from './api/provide-api';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideApi('')]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render backend response', async () => {
    const fixture = TestBed.createComponent(App);
    const httpTesting = TestBed.inject(HttpTestingController);
    const request = httpTesting.expectOne('/api/hello');
    request.flush({
      message: 'Hello from Spring Boot',
      timestamp: '2026-06-18T12:00:00Z'
    });

    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Hello from Spring Boot');
    expect(compiled.querySelector('.meta')?.textContent).toContain('2026-06-18T12:00:00Z');
  });
});
