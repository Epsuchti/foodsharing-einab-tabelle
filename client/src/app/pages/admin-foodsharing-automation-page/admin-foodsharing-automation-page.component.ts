import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminService,
  AutomationRunSummary,
  FoodsharingAutomationAudit,
  FoodsharingExtraAutomationAudit,
  FoodsharingExtraAutomationOverview,
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
  protected readonly extraAutomationAudit = signal<FoodsharingExtraAutomationAudit[]>([]);
  protected readonly foodsharingFuturePickupUsers = signal<FoodsharingFuturePickupUser[]>([]);
  protected readonly cleaningRuleExemptions = signal<FoodsharingCleaningRuleExemption[]>([]);
  protected readonly foodsharingEmail = signal('');
  protected readonly cleaningExemptionFoodsharingId = signal('');
  protected readonly cleaningExemptionReason = signal('');
  protected readonly foodsharingRunResult = signal<FoodsharingRunResult | null>(null);
  protected readonly onlyMyAutomations = signal(true);
  protected readonly selectedStoreId = signal<number | null>(null);
  protected readonly telegramChatOptions = signal<{ label: string; value: string }[]>([]);
  protected readonly advertNumbers = [1, 2, 3];
  protected telegramBotToken = "";
  protected readonly visibleFoodsharingStores = computed(() => this.onlyMyAutomations()
    ? this.foodsharingStores().filter((store) => store.editable)
    : this.foodsharingStores());
  protected readonly availableStoreOptions = computed(() => this.availableStores().map((store) => ({
    label: `${store.storeName} (${store.storeId})`,
    value: store.storeId
  })));
  protected foodsharingPassword = '';

  private readonly adminApi = inject(AdminService);
  protected readonly sessionService = inject(SessionService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.loadFoodsharingAutomation();
    this.loadCleaningRuleExemptions();
  }

  connectFoodsharing(): void {
    this.adminApi.connectFoodsharing({
      foodsharingConnectRequest: {
        email: this.foodsharingEmail(),
        password: this.foodsharingPassword,
        telegramBotToken: this.telegramBotToken || undefined
      }
    }).subscribe({
      next: (status) => {
        this.foodsharingStatus.set(status);
        this.foodsharingPassword = '';
        this.telegramBotToken = '';
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
        this.extraAutomationAudit.set([]);
        this.foodsharingRunResult.set(null);
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  saveRequestAutomation(store: FoodsharingStoreAutomation & { requestEnabled?: boolean; requestDryRunEnabled?: boolean }): void {
    this.adminApi.saveFoodsharingRequestAutomation({
      storeId: store.storeId,
      foodsharingRequestAutomationRequest: {
        storeName: store.storeName,
        enabled: !!store.requestEnabled,
        dryRunEnabled: store.requestDryRunEnabled !== false
      }
    }).subscribe({ next: () => this.messageService.add({ severity: 'success', summary: this.i18n.t('common.saved') }), error: (error) => this.toastError(resolveApiError(error, this.i18n)) });
  }

  previewOpenSlotAdvertisement(store: FoodsharingStoreAutomation, advertNumber: number): string {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const firstTemplate = String(data[`advertMessages${advertNumber}`] || '').split('\n---\n').map((message) => message.trim()).filter(Boolean)[0];
    if (!firstTemplate) {
      return 'Add at least one message to preview it here.';
    }
    const sampleDate = new Date(Date.now() + 24 * 60 * 60 * 1000);
    const date = sampleDate.toISOString().slice(0, 10);
    const time = sampleDate.toTimeString().slice(0, 5);
    const adminFoodsharingId = this.sessionService.foodsharingId() || '';
    const dateDe = sampleDate.toLocaleDateString('de-CH', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const weekday = sampleDate.toLocaleDateString('de-CH', { weekday: 'long' });
    return firstTemplate
      .replaceAll('{{storeName}}', store.storeName || '')
      .replaceAll('{{date}}', date)
      .replaceAll('{{dateDe}}', dateDe)
      .replaceAll('{{weekday}}', weekday)
      .replaceAll('{{time}}', time)
      .replaceAll('{{datetime}}', `${date} ${time}`)
      .replaceAll('{{datetimeDe}}', `${weekday}, ${dateDe} um ${time}`)
      .replaceAll('{{adminFoodsharingId}}', adminFoodsharingId)
      .replaceAll('{{adminProfileUrl}}', adminFoodsharingId ? `https://foodsharing.de/user/${adminFoodsharingId}/profile` : '');
  }

  saveOpenSlotAdvertisement(store: FoodsharingStoreAutomation, advertNumber: number): void {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const messages = String(data[`advertMessages${advertNumber}`] || '').split('\n---\n').map((message) => message.trim()).filter(Boolean);
    const sendToStoreChat = Boolean(data[`advertSendToStoreChat${advertNumber}`]);
    const sendToTelegram = Boolean(data[`advertSendToTelegram${advertNumber}`]);
    const telegramChatId = String(data[`advertTelegramChatId${advertNumber}`] || '').trim();
    if (!sendToStoreChat && !sendToTelegram) {
      this.toastError('Select store chat, Telegram, or both.');
      return;
    }
    if (sendToTelegram && !telegramChatId) {
      this.toastError('Select a Telegram chat or enter a chat id.');
      return;
    }
    if (messages.length === 0) {
      this.toastError('Add at least one advert message.');
      return;
    }
    this.adminApi.saveFoodsharingOpenSlotAdvertisementAutomation({
      storeId: store.storeId,
      advertNumber,
      foodsharingOpenSlotAdvertisementAutomationRequest: {
        storeName: store.storeName,
        enabled: Boolean(data[`advertEnabled${advertNumber}`]),
        triggerHoursBefore: Number(data[`advertHoursBefore${advertNumber}`] || (advertNumber === 1 ? 24 : advertNumber === 2 ? 12 : 3)),
        sendToStoreChat,
        sendToTelegram,
        telegramChatId: telegramChatId || undefined,
        messages
      }
    }).subscribe({ next: () => this.messageService.add({ severity: 'success', summary: this.i18n.t('common.saved') }), error: (error) => this.toastError(resolveApiError(error, this.i18n)) });
  }

  runRequestAutomationDryRun(): void {
    this.adminApi.runFoodsharingRequestAutomationDryRun().subscribe({
      next: (result) => this.foodsharingRunResult.set(this.toRunResult(result)),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  runAdvertisementAutomationDryRun(): void {
    this.adminApi.runFoodsharingOpenSlotAdvertisementDryRun().subscribe({
      next: (result) => this.foodsharingRunResult.set(this.toRunResult(result)),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }


  sendTelegramTestMessage(store: FoodsharingStoreAutomation, advertNumber: number): void {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const chatId = String(data[`advertTelegramChatId${advertNumber}`] || '').trim();
    if (!chatId) {
      this.toastError('Select a Telegram chat or enter a chat id.');
      return;
    }
    this.adminApi.sendFoodsharingTelegramTestMessage({
      telegramTestMessageRequest: {
        chatId,
        message: this.previewOpenSlotAdvertisement(store, advertNumber)
      }
    }).subscribe({
      next: () => this.messageService.add({ severity: 'success', summary: 'Telegram test message sent' }),
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
              this.loadExtraAutomationOverview();
            },
            error: () => undefined
          });
          this.loadFoodsharingFuturePickupUsers();
          this.loadFoodsharingAudit();
          this.loadExtraAutomationAudit();
          this.loadTelegramChats();
        } else {
          this.foodsharingStores.set([]);
          this.availableStores.set([]);
          this.foodsharingFuturePickupUsers.set([]);
          this.foodsharingAudit.set([]);
          this.extraAutomationAudit.set([]);
          this.telegramChatOptions.set([]);
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


  private loadExtraAutomationOverview(): void {
    this.adminApi.getFoodsharingExtraAutomationOverview().subscribe({
      next: (overview: FoodsharingExtraAutomationOverview) => {
        const stores = this.foodsharingStores().map((store) => ({ ...store }) as FoodsharingStoreAutomation & Record<string, unknown>);
        for (const requestAutomation of overview.requestAutomations || []) {
          const store = stores.find((entry) => entry.storeId === requestAutomation.storeId);
          if (store) {
            store['requestEnabled'] = requestAutomation.enabled;
            store['requestDryRunEnabled'] = requestAutomation.dryRunEnabled;
          }
        }
        for (const advert of overview.advertisementAutomations || []) {
          const store = stores.find((entry) => entry.storeId === advert.storeId);
          if (store) {
            const number = advert.advertNumber;
            store[`advertEnabled${number}`] = advert.enabled;
            store[`advertHoursBefore${number}`] = advert.triggerHoursBefore;
            store[`advertSendToStoreChat${number}`] = advert.sendToStoreChat;
            store[`advertSendToTelegram${number}`] = advert.sendToTelegram;
            store[`advertTelegramChatId${number}`] = advert.telegramChatId;
            store[`advertMessages${number}`] = (advert.messages || []).join('\n---\n');
          }
        }
        this.foodsharingStores.set(stores as FoodsharingStoreAutomation[]);
      },
      error: () => undefined
    });
  }


  refreshTelegramChats(): void {
    this.loadTelegramChats();
  }

  private loadExtraAutomationAudit(): void {
    if (!this.sessionService.hasPermission(UserPermission.CanSeeAllAutomationDecisions)) {
      this.extraAutomationAudit.set([]);
      return;
    }
    this.adminApi.getFoodsharingExtraAutomationAudit().subscribe({
      next: (audit) => this.extraAutomationAudit.set(audit),
      error: () => undefined
    });
  }

  private loadTelegramChats(): void {
    if (!this.sessionService.hasPermission(UserPermission.CanUseAutomationOpenSlotAdvertising)) {
      this.telegramChatOptions.set([]);
      return;
    }
    this.adminApi.getFoodsharingTelegramChats().subscribe({
      next: (chats) => this.telegramChatOptions.set(chats.map((chat) => ({ label: `${chat.title} (${chat.type || chat.id})`, value: chat.id }))),
      error: () => this.telegramChatOptions.set([])
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

  private toRunResult(result: AutomationRunSummary): FoodsharingRunResult {
    return {
      evaluated: result.evaluated,
      confirmed: result.acted,
      declined: result.skipped,
      failed: 0,
      dryRun: result.dryRun,
      messages: result.messages || []
    };
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}
