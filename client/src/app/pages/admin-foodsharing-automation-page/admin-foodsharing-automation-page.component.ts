import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminService,
  FoodsharingAutomationAudit,
  FoodsharingConnectionStatus,
  FoodsharingFuturePickupUser,
  FoodsharingRunResult,
  FoodsharingStoreAutomation
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';

@Component({
  selector: 'app-admin-foodsharing-automation-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    CardModule,
    CheckboxModule,
    InputNumberModule,
    InputTextModule,
    ZurichDateTimePipe,
    TableModule,
    TagModule
  ],
  templateUrl: './admin-foodsharing-automation-page.component.html'
})
export class AdminFoodsharingAutomationPageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly foodsharingStatus = signal<FoodsharingConnectionStatus | null>(null);
  protected readonly foodsharingStores = signal<FoodsharingStoreAutomation[]>([]);
  protected readonly foodsharingAudit = signal<FoodsharingAutomationAudit[]>([]);
  protected readonly foodsharingFuturePickupUsers = signal<FoodsharingFuturePickupUser[]>([]);
  protected readonly foodsharingEmail = signal('');
  protected readonly foodsharingRunResult = signal<FoodsharingRunResult | null>(null);
  protected foodsharingPassword = '';

  private readonly adminApi = inject(AdminService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.loadFoodsharingAutomation();
  }

  connectFoodsharing(): void {
    this.adminApi.connectFoodsharing({
      foodsharingConnectRequest: {
        email: this.foodsharingEmail(),
        password: this.foodsharingPassword
      }
    }).subscribe({
      next: (status) => {
        this.foodsharingStatus.set(status);
        this.foodsharingPassword = '';
        this.loadFoodsharingAutomation();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  disconnectFoodsharing(): void {
    this.adminApi.disconnectFoodsharing().subscribe({
      next: () => {
        this.foodsharingStatus.set({ connected: false });
        this.foodsharingStores.set([]);
        this.foodsharingFuturePickupUsers.set([]);
        this.foodsharingAudit.set([]);
        this.foodsharingRunResult.set(null);
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  saveFoodsharingStore(store: FoodsharingStoreAutomation): void {
    this.adminApi.saveFoodsharingStoreAutomation({
      storeId: store.storeId,
      foodsharingStoreAutomationRequest: store
    }).subscribe({
      next: () => this.loadFoodsharingAutomation(),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  runFoodsharingDryRun(): void {
    this.adminApi.runFoodsharingAutomation({ foodsharingRunRequest: { dryRun: true } }).subscribe({
      next: (result) => {
        this.foodsharingRunResult.set(result);
        this.loadFoodsharingAudit();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  private loadFoodsharingAutomation(): void {
    this.adminApi.getFoodsharingStatus().subscribe({
      next: (status) => {
        this.foodsharingStatus.set(status);
        if (status.connected) {
          this.adminApi.getFoodsharingStores().subscribe({
            next: (stores) => this.foodsharingStores.set(stores),
            error: () => undefined
          });
          this.loadFoodsharingFuturePickupUsers();
          this.loadFoodsharingAudit();
        } else {
          this.foodsharingStores.set([]);
          this.foodsharingFuturePickupUsers.set([]);
          this.foodsharingAudit.set([]);
        }
      },
      error: () => undefined
    });
  }

  private loadFoodsharingFuturePickupUsers(): void {
    this.adminApi.getFoodsharingFuturePickupUsers().subscribe({
      next: (users) => this.foodsharingFuturePickupUsers.set(users),
      error: () => undefined
    });
  }

  private loadFoodsharingAudit(): void {
    this.adminApi.getFoodsharingAutomationAudit().subscribe({
      next: (audit) => this.foodsharingAudit.set(audit),
      error: () => undefined
    });
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}
