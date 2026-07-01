import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { AuthResponse, AuthService, LoginRequest, MessageResponse, VerifyTokenRequest } from '../api';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class AuthFacadeService {
  constructor(
    private readonly authApi: AuthService,
    private readonly sessionService: SessionService
  ) {}

  requestLogin(foodsharingId: string): Observable<MessageResponse> {
    const loginRequest: LoginRequest = { foodsharingId };
    return this.authApi.requestLogin({ loginRequest });
  }

  verifyToken(token: string): Observable<AuthResponse> {
    const verifyTokenRequest: VerifyTokenRequest = { token };
    return this.authApi.verifyLoginToken({ verifyTokenRequest }).pipe(
      tap((response) => this.sessionService.setSession(response))
    );
  }
}
