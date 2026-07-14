import { Injectable, inject, signal } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { Observable, map, of, tap } from 'rxjs';

import { BezirkResponse, PublicService } from '../api';

@Injectable({ providedIn: 'root' })
export class BezirkContextService {
  readonly bezirke = signal<BezirkResponse[]>([]);
  readonly selectedBezirk = signal<BezirkResponse | null>(null);

  private readonly publicApi = inject(PublicService);
  private readonly router = inject(Router);

  loadBezirke(): Observable<BezirkResponse[]> {
    const current = this.bezirke();
    if (current.length) {
      return of(current);
    }
    return this.publicApi.getBezirke().pipe(
      map((response) => response.bezirke),
      tap((bezirke) => this.bezirke.set(bezirke))
    );
  }

  selectBezirk(slug: string | null): Observable<true | UrlTree> {
    if (!slug) {
      this.selectedBezirk.set(null);
      return of(this.router.createUrlTree(['/']));
    }
    return this.loadBezirke().pipe(
      map((bezirke) => {
        const bezirk = bezirke.find((entry) => entry.slug === slug);
        if (!bezirk) {
          this.selectedBezirk.set(null);
          return this.router.createUrlTree(['/']);
        }
        this.selectedBezirk.set(bezirk);
        return true as const;
      })
    );
  }

  currentSlug(): string {
    const slug = this.selectedBezirk()?.slug;
    if (!slug) {
      throw new Error('No Bezirk is selected.');
    }
    return slug;
  }

  route(...segments: string[]): string[] {
    const slug = this.selectedBezirk()?.slug;
    return slug ? ['/bezirke', slug, ...segments] : ['/'];
  }
}
