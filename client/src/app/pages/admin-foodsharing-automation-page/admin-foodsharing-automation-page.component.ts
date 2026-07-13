import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminService,
  FoodsharingAutomationAudit,
  FoodsharingCleaningRuleExemption,
  FoodsharingConnectionStatus,
  FoodsharingFuturePickupUser,
  FoodsharingManagedStore,
  FoodsharingRunResult,
  FoodsharingStoreAutomation,
  FoodsharingStoreAutomationOverview,
  UserPermission
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { SessionService } from '../../core/session.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TabsModule } from 'primeng/tabs';
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
    SelectModule,
    ZurichDateTimePipe,
    TableModule,
    TagModule,
    TabsModule
  ],
  templateUrl: './admin-foodsharing-automation-page.component.html',
  styleUrl: './admin-foodsharing-automation-page.component.scss'
})
export class AdminFoodsharingAutomationPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  protected readonly UserPermission = UserPermission;

  protected readonly foodsharingStatus = signal<FoodsharingConnectionStatus | null>(null);
  protected readonly foodsharingStores = signal<FoodsharingStoreAutomation[]>([]);
  protected readonly availableStores = signal<FoodsharingManagedStore[]>([]);
  protected readonly foodsharingAudit = signal<FoodsharingAutomationAudit[]>([]);
  protected readonly foodsharingFuturePickupUsers = signal<FoodsharingFuturePickupUser[]>([]);
  protected readonly cleaningRuleExemptions = signal<FoodsharingCleaningRuleExemption[]>([]);
  protected readonly foodsharingEmail = signal('');
  protected readonly cleaningExemptionFoodsharingId = signal('');
  protected readonly cleaningExemptionReason = signal('');
  protected readonly foodsharingRunResult = signal<FoodsharingRunResult | null>(null);
  protected readonly onlyMyAutomations = signal(true);
  protected readonly selectedStoreId = signal<number | null>(null);
  protected readonly visibleFoodsharingStores = computed(() => this.onlyMyAutomations()
    ? this.foodsharingStores().filter((store) => store.editable)
    : this.foodsharingStores());
  protected readonly canManageAutomation = computed(() =>
    this.sessionService.hasPermission(UserPermission.CanUseAutomations)
    || this.sessionService.hasPermission(UserPermission.CanUseAutomationSlotApproval));
  protected readonly availableStoreOptions = computed(() => this.availableStores().map((store) => ({
    label: `${store.storeName} (${store.storeId})`,
    value: store.storeId
  })));
  protected foodsharingPassword = '';

  private readonly adminApi = inject(AdminService);
  protected readonly sessionService = inject(SessionService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    if (this.canManageAutomation()) {
      this.loadFoodsharingAutomation();
    } else {
      if (this.sessionService.hasPermission(UserPermission.CanSeeUserPickupCountGrouping)) {
        this.loadFoodsharingFuturePickupUsers();
      }
      if (this.sessionService.hasPermission(UserPermission.CanSeeAllAutomationDecisions)) {
        this.loadFoodsharingAudit();
      }
    }
    this.loadCleaningRuleExemptions();
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
        this.availableStores.set([]);
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

  addFoodsharingStore(): void {
    const storeId = this.selectedStoreId();
    const store = this.availableStores().find((entry) => entry.storeId === storeId);
    if (!store) {
      return;
    }
    this.adminApi.saveFoodsharingStoreAutomation({
      storeId: store.storeId,
      foodsharingStoreAutomationRequest: {
        storeName: store.storeName,
        enabled: false,
        dryRunEnabled: true,
        gapRuleEnabled: false,
        minimumGapDays: 0,
        cleaningRuleEnabled: false,
        experienceRuleEnabled: false
      }
    }).subscribe({
      next: () => {
        this.selectedStoreId.set(null);
        this.loadFoodsharingAutomation();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  storeOwnerLabel(store: FoodsharingStoreAutomation): string {
    if (!store.ownerName && !store.ownerEmail) {
      return this.i18n.t('adminAutomation.available');
    }
    if (store.ownerName && store.ownerEmail) {
      return `${store.ownerName} (${store.ownerEmail})`;
    }
    return store.ownerName || store.ownerEmail || this.i18n.t('adminAutomation.available');
  }

  statusBannerText(): string {
    const status = this.foodsharingStatus();
    if (!status) {
      return '';
    }
    if (!status.automationEnabled) {
      return this.i18n.t('adminAutomation.modeDisabled');
    }
    return status.automationDryRun
      ? this.i18n.t('adminAutomation.modeDryRun')
      : this.i18n.t('adminAutomation.modeActive');
  }

  statusBannerClass(): string {
    const status = this.foodsharingStatus();
    if (!status) {
      return '';
    }
    if (!status.automationEnabled) {
      return 'automation-mode-banner--disabled';
    }
    return status.automationDryRun ? 'automation-mode-banner--dry-run' : 'automation-mode-banner--active';
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


  saveCleaningRuleExemption(): void {
    this.adminApi.saveFoodsharingCleaningRuleExemption({
      foodsharingCleaningRuleExemptionRequest: {
        foodsharingId: this.cleaningExemptionFoodsharingId(),
        reason: this.cleaningExemptionReason()
      }
    }).subscribe({
      next: () => {
        this.cleaningExemptionFoodsharingId.set('');
        this.cleaningExemptionReason.set('');
        this.loadCleaningRuleExemptions();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  deleteCleaningRuleExemption(exemption: FoodsharingCleaningRuleExemption): void {
    this.adminApi.deleteFoodsharingCleaningRuleExemption({ exemptionId: exemption.id }).subscribe({
      next: () => this.loadCleaningRuleExemptions(),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  private loadFoodsharingAutomation(): void {
    this.adminApi.getFoodsharingStatus().subscribe({
      next: (status) => {
        this.foodsharingStatus.set(status);
        if (status.connected) {
          this.adminApi.getFoodsharingStores().subscribe({
            next: (overview: FoodsharingStoreAutomationOverview) => {
              this.foodsharingStores.set(overview.automations);
              this.availableStores.set(overview.availableStores);
            },
            error: () => undefined
          });
          this.loadFoodsharingFuturePickupUsers();
          this.loadFoodsharingAudit();
        } else {
          this.foodsharingStores.set([]);
          this.availableStores.set([]);
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

  private loadCleaningRuleExemptions(): void {
    if (!this.sessionService.hasPermission(UserPermission.CanUseAutomationSlotApproval)) {
      this.cleaningRuleExemptions.set([]);
      return;
    }
    this.adminApi.getFoodsharingCleaningRuleExemptions().subscribe({
      next: (exemptions) => this.cleaningRuleExemptions.set(exemptions),
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
