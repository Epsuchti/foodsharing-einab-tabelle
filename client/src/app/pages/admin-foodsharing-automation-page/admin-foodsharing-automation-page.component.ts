import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminService,
  AutomationRunSummary,
  FoodsharingAutomationAudit,
  FoodsharingExtraAutomationAudit,
  FoodsharingCleaningRuleExemption,
  FoodsharingConnectionStatus,
  FoodsharingFuturePickupUser,
  FoodsharingManagedStore,
  FoodsharingOpenSlotAdvertisementOverview,
  FoodsharingRunResult,
  FoodsharingRequestAutomationOverview,
  FoodsharingStoreAutomation,
  FoodsharingStoreAutomationOverview,
  UserPermission
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { BezirkContextService } from '../../core/bezirk-context.service';
import { I18nService } from '../../core/i18n.service';
import { SessionService } from '../../core/session.service';
import { ButtonModule } from 'primeng/button';
import { AccordionModule } from 'primeng/accordion';
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
    AccordionModule,
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
  protected readonly requestAutomationAudit = signal<FoodsharingExtraAutomationAudit[]>([]);
  protected readonly advertisementAutomationAudit = signal<FoodsharingExtraAutomationAudit[]>([]);
  protected readonly foodsharingFuturePickupUsers = signal<FoodsharingFuturePickupUser[]>([]);
  protected readonly cleaningRuleExemptions = signal<FoodsharingCleaningRuleExemption[]>([]);
  protected readonly cleaningStoreConfigured = signal<boolean | null>(null);
  protected readonly foodsharingEmail = signal('');
  protected readonly cleaningExemptionFoodsharingId = signal('');
  protected readonly cleaningExemptionReason = signal('');
  protected readonly slotRunResult = signal<FoodsharingRunResult | null>(null);
  protected readonly requestRunResult = signal<FoodsharingRunResult | null>(null);
  protected readonly advertisementRunResult = signal<FoodsharingRunResult | null>(null);
  protected readonly onlyMyAutomations = signal(true);
  protected readonly onlyMyDecisions = signal(true);
  protected readonly auditConsoleExpanded = signal(false);
  protected readonly activeAutomationTab = signal('slots');
  protected readonly selectedStoreId = signal<number | null>(null);
  protected readonly selectedRequestStoreId = signal<number | null>(null);
  protected readonly selectedAdvertisementStoreId = signal<number | null>(null);
  protected readonly telegramChatOptions = signal<{ label: string; value: string }[]>([]);
  protected readonly advertisementMessageTabs = signal<Record<string, 'store' | 'telegram' | 'preview'>>({});
  protected telegramBotToken = "";
  protected readonly telegramBotTokenConfigured = computed(() => this.foodsharingStatus()?.telegramBotTokenConfigured ?? false);
  protected readonly visibleFoodsharingStores = computed(() => this.onlyMyAutomations()
    ? this.foodsharingStores().filter((store) => store.editable)
    : this.foodsharingStores());
  protected readonly slotApprovalStores = computed(() => this.foodsharingStores()
    .filter((store) => Boolean((store as FoodsharingStoreAutomation & Record<string, unknown>)['slotApprovalConfigured'])));
  protected readonly requestAutomationStores = computed(() => this.visibleFoodsharingStores()
    .filter((store) => Boolean((store as FoodsharingStoreAutomation & Record<string, unknown>)['requestConfigured'])));
  protected readonly advertisementAutomationStores = computed(() => this.visibleFoodsharingStores()
    .filter((store) => this.advertisementNumbers(store).length > 0));
  protected readonly plannedAdvertisementNotifications = computed(() => this.advertisementAutomationAudit()
    .filter((entry) => entry.status === 'PLANNED'));
  protected readonly sentAdvertisementAudit = computed(() => this.advertisementAutomationAudit()
    .filter((entry) => entry.status !== 'PLANNED'));
  protected readonly availableStoreOptions = computed(() => this.availableStores().map((store) => ({
    label: `${store.storeName} (${store.storeId})`,
    value: store.storeId
  })));
  protected readonly availableRequestStoreOptions = computed(() => this.foodsharingStores()
    .filter((store) => !(store as FoodsharingStoreAutomation & Record<string, unknown>)['requestConfigured'])
    .map((store) => ({ label: `${store.storeName} (${store.storeId})`, value: store.storeId })));
  protected readonly availableAdvertisementStoreOptions = computed(() => this.foodsharingStores()
    .filter((store) => this.advertisementNumbers(store).length === 0)
    .map((store) => ({ label: `${store.storeName} (${store.storeId})`, value: store.storeId })));
  protected foodsharingPassword = '';

  private readonly adminApi = inject(AdminService);
  private readonly bezirkContext = inject(BezirkContextService);
  protected readonly sessionService = inject(SessionService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.activeAutomationTab.set(this.sessionService.hasPermission(UserPermission.CanUseAutomationSlotApproval)
      ? 'slots'
      : this.sessionService.hasPermission(UserPermission.CanUseAutomationRequestApproval)
        ? 'applications'
        : this.sessionService.hasPermission(UserPermission.CanUseAutomationOpenSlotAdvertising) ? 'advertisements' : 'statistics');
    this.loadBezirkSettings();
    this.loadFoodsharingStatus();
  }

  onAutomationTabChange(tab: string | number | undefined): void {
    if (tab == null) {
      return;
    }
    const nextTab = String(tab);
    this.activeAutomationTab.set(nextTab);
    this.loadActiveAutomationTab();
  }

  setOnlyMyDecisions(onlyMyDecisions: boolean): void {
    this.onlyMyDecisions.set(onlyMyDecisions);
    this.loadActiveAutomationTab();
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
        this.loadActiveAutomationTab();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  saveTelegramBotToken(): void {
    this.adminApi.saveFoodsharingTelegramBotToken({
      foodsharingTelegramBotTokenRequest: {
        telegramBotToken: this.telegramBotToken
      }
    }).subscribe({
      next: () => {
        this.telegramBotToken = '';
        this.messageService.add({ severity: 'success', summary: this.i18n.t('common.saved') });
        this.loadFoodsharingStatus();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  removeTelegramBotToken(): void {
    this.adminApi.saveFoodsharingTelegramBotToken({
      foodsharingTelegramBotTokenRequest: {
        telegramBotToken: ''
      }
    }).subscribe({
      next: () => {
        this.telegramBotToken = '';
        this.messageService.add({ severity: 'success', summary: this.i18n.t('common.deleted') });
        this.loadFoodsharingStatus();
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
        this.requestAutomationAudit.set([]);
        this.advertisementAutomationAudit.set([]);
        this.slotRunResult.set(null);
        this.requestRunResult.set(null);
        this.advertisementRunResult.set(null);
        this.telegramBotToken = '';
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  saveRequestAutomation(store: FoodsharingStoreAutomation & { requestEnabled?: boolean; requestDryRunEnabled?: boolean; requestDistanceRuleEnabled?: boolean; requestMaximumDistanceKm?: number }): void {
    this.adminApi.saveFoodsharingRequestAutomation({
      storeId: store.storeId,
      foodsharingRequestAutomationRequest: {
        storeName: store.storeName,
        enabled: !!store.requestEnabled,
        dryRunEnabled: store.requestDryRunEnabled !== false,
        distanceRuleEnabled: store.requestDistanceRuleEnabled === true,
        maximumDistanceKm: Number(store.requestMaximumDistanceKm ?? 0)
      }
    }).subscribe({ next: () => this.messageService.add({ severity: 'success', summary: this.i18n.t('common.saved') }), error: (error) => this.toastError(resolveApiError(error, this.i18n)) });
  }

  deleteRequestAutomation(store: FoodsharingStoreAutomation): void {
    if (!window.confirm(this.i18n.t('common.deleteConfirm'))) {
      return;
    }
    this.adminApi.deleteFoodsharingRequestAutomation({ storeId: store.storeId }).subscribe({
      next: () => this.reloadActiveAutomationTab(),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  addRequestAutomation(): void {
    const storeId = this.selectedRequestStoreId();
    const store = this.foodsharingStores().find((entry) => entry.storeId === storeId);
    if (!store) {
      return;
    }
    this.adminApi.saveFoodsharingRequestAutomation({
      storeId: store.storeId,
      foodsharingRequestAutomationRequest: {
        storeName: store.storeName,
        enabled: false,
        dryRunEnabled: true,
        distanceRuleEnabled: false,
        maximumDistanceKm: 0
      }
    }).subscribe({
      next: () => {
        this.selectedRequestStoreId.set(null);
        this.reloadActiveAutomationTab();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  previewOpenSlotAdvertisement(store: FoodsharingStoreAutomation, advertNumber: number, channel: 'store' | 'telegram'): string {
    const templates = this.advertisementMessages(store, advertNumber, channel).map((message) => message.trim()).filter(Boolean);
    if (templates.length === 0) {
      return this.i18n.t('automation.addAdvertMessagePreview');
    }
    const sampleDate = new Date(Date.now() + 24 * 60 * 60 * 1000);
    const date = sampleDate.toISOString().slice(0, 10);
    const time = sampleDate.toTimeString().slice(0, 5);
    const adminFoodsharingId = this.sessionService.foodsharingId() || '';
    const dateDe = sampleDate.toLocaleDateString('de-CH', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const weekday = sampleDate.toLocaleDateString('de-CH', { weekday: 'long' });
    return templates.map((template) => template
      .replaceAll('{{date}}', date)
      .replaceAll('{{dateDe}}', dateDe)
      .replaceAll('{{weekday}}', weekday)
      .replaceAll('{{time}}', time)
      .replaceAll('{{datetime}}', `${date} ${time}`)
      .replaceAll('{{datetimeDe}}', `${weekday}, ${dateDe} um ${time}`)
      .replaceAll('{{adminFoodsharingId}}', adminFoodsharingId)
      .replaceAll('{{adminProfileUrl}}', adminFoodsharingId ? `https://foodsharing.de/user/${adminFoodsharingId}/profile` : '')
    ).join('\n\n---\n\n');
  }

  advertisementMessages(store: FoodsharingStoreAutomation, advertNumber: number, channel: 'store' | 'telegram'): string[] {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const propertyName = channel === 'store' ? `advertStoreMessages${advertNumber}` : `advertTelegramMessages${advertNumber}`;
    const messages = String(data[propertyName] || '').split('\n---\n').map((message) => message.trim());
    return messages.length > 0 ? messages : [''];
  }

  advertisementMessageTab(store: FoodsharingStoreAutomation, advertNumber: number): 'store' | 'telegram' | 'preview' {
    const tabKey = this.advertisementMessageTabKey(store, advertNumber);
    const tab = this.advertisementMessageTabs()[tabKey] ?? 'store';
    return tab === 'telegram' && !this.telegramBotTokenConfigured() ? 'store' : tab;
  }

  setAdvertisementMessageTab(store: FoodsharingStoreAutomation, advertNumber: number, tab: string | number | undefined): void {
    if (tab !== 'store' && tab !== 'telegram' && tab !== 'preview') {
      return;
    }
    if (tab === 'telegram' && !this.telegramBotTokenConfigured()) {
      return;
    }
    const tabKey = this.advertisementMessageTabKey(store, advertNumber);
    this.advertisementMessageTabs.update((tabs) => ({ ...tabs, [tabKey]: tab }));
  }

  updateAdvertisementMessage(store: FoodsharingStoreAutomation, advertNumber: number, channel: 'store' | 'telegram', index: number, value: string): void {
    const messages = this.advertisementMessages(store, advertNumber, channel);
    messages[index] = value;
    this.setAdvertisementMessages(store, advertNumber, channel, messages);
  }

  addAdvertisementMessage(store: FoodsharingStoreAutomation, advertNumber: number, channel: 'store' | 'telegram'): void {
    const messages = this.advertisementMessages(store, advertNumber, channel);
    messages.push('');
    this.setAdvertisementMessages(store, advertNumber, channel, messages);
  }

  removeAdvertisementMessage(store: FoodsharingStoreAutomation, advertNumber: number, channel: 'store' | 'telegram', index: number): void {
    const messages = this.advertisementMessages(store, advertNumber, channel);
    if (messages.length === 1) {
      return;
    }
    messages.splice(index, 1);
    this.setAdvertisementMessages(store, advertNumber, channel, messages);
  }

  saveOpenSlotAdvertisement(store: FoodsharingStoreAutomation, advertNumber: number): void {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const storeMessages = this.advertisementMessages(store, advertNumber, 'store').map((message) => message.trim()).filter(Boolean);
    const telegramMessages = this.advertisementMessages(store, advertNumber, 'telegram').map((message) => message.trim()).filter(Boolean);
    const sendToStoreChat = Boolean(data[`advertSendToStoreChat${advertNumber}`]);
    const sendToTelegram = Boolean(data[`advertSendToTelegram${advertNumber}`]);
    const telegramChatId = String(data[`advertTelegramChatId${advertNumber}`] || '').trim();
    const lateCancellationMessage = String(data[`advertLateCancellationMessage${advertNumber}`] || '').trim();
    const sendLateCancellationMessage = Boolean(data[`advertSendLateCancellationMessage${advertNumber}`]);
    const sendLatestAdvertisementAfterLateCancellation = Boolean(data[`advertSendLatestAdvertisementAfterLateCancellation${advertNumber}`]);
    if (!sendToStoreChat && !sendToTelegram) {
      this.toastError(this.i18n.t('automation.selectDestination'));
      return;
    }
    if (sendToStoreChat && storeMessages.length === 0) {
      this.toastError(this.i18n.t('automation.addStoreChatMessage'));
      return;
    }
    if (sendToTelegram && !telegramChatId) {
      this.toastError(this.i18n.t('automation.selectTelegramChat'));
      return;
    }
    if (sendToTelegram && telegramMessages.length === 0) {
      this.toastError(this.i18n.t('automation.addTelegramMessage'));
      return;
    }
    if (!lateCancellationMessage) {
      this.toastError(this.i18n.t('automation.lateCancellationMessageRequired'));
      return;
    }
    this.adminApi.saveFoodsharingOpenSlotAdvertisementAutomation({
      storeId: store.storeId,
      advertNumber,
      foodsharingOpenSlotAdvertisementAutomationRequest: {
        storeName: store.storeName,
        enabled: Boolean(data[`advertEnabled${advertNumber}`]),
        dryRunEnabled: Boolean(data[`advertDryRunEnabled${advertNumber}`]),
        triggerHoursBefore: Number(data[`advertHoursBefore${advertNumber}`] || (advertNumber === 1 ? 24 : advertNumber === 2 ? 12 : 3)),
        sendToStoreChat,
        sendToTelegram,
        telegramChatId: telegramChatId || undefined,
        storeMessages,
        telegramMessages,
        lateCancellationMessage,
        sendLateCancellationMessage,
        sendLatestAdvertisementAfterLateCancellation
      }
    }).subscribe({ next: () => this.messageService.add({ severity: 'success', summary: this.i18n.t('common.saved') }), error: (error) => this.toastError(resolveApiError(error, this.i18n)) });
  }

  deleteAdvertisementAutomation(store: FoodsharingStoreAutomation, advertNumber: number): void {
    if (!window.confirm(this.i18n.t('common.deleteConfirm'))) {
      return;
    }
    this.adminApi.deleteFoodsharingOpenSlotAdvertisementAutomation({ storeId: store.storeId, advertNumber }).subscribe({
      next: () => this.reloadActiveAutomationTab(),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  private setAdvertisementMessages(store: FoodsharingStoreAutomation, advertNumber: number, channel: 'store' | 'telegram', messages: string[]): void {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const propertyName = channel === 'store' ? `advertStoreMessages${advertNumber}` : `advertTelegramMessages${advertNumber}`;
    data[propertyName] = messages.join('\n---\n');
    this.foodsharingStores.update((stores) => [...stores]);
  }

  private advertisementMessageTabKey(store: FoodsharingStoreAutomation, advertNumber: number): string {
    return `${store.storeId}-${advertNumber}`;
  }

  runRequestAutomationDryRun(): void {
    this.adminApi.runFoodsharingRequestAutomationDryRun().subscribe({
      next: (result) => {
        this.requestRunResult.set(this.toRunResult(result));
        this.loadRequestAutomationAuditIfActive();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  runAdvertisementAutomationDryRun(): void {
    this.adminApi.runFoodsharingOpenSlotAdvertisementDryRun().subscribe({
      next: (result) => {
        this.advertisementRunResult.set(this.toRunResult(result));
        this.loadAdvertisementAutomationAuditIfActive();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  saveFoodsharingStore(store: FoodsharingStoreAutomation): void {
    this.adminApi.saveFoodsharingStoreAutomation({
      bezirkSlug: this.bezirkContext.currentSlug(),
      storeId: store.storeId,
      foodsharingStoreAutomationRequest: store
    }).subscribe({
      next: (savedStore) => {
        this.foodsharingStores.update((stores) => stores.map((entry) => entry.storeId === savedStore.storeId
          ? { ...entry, ...savedStore, slotApprovalConfigured: true }
          : entry));
        this.messageService.add({ severity: 'success', summary: this.i18n.t('common.saved') });
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  deleteFoodsharingStore(store: FoodsharingStoreAutomation): void {
    if (!window.confirm(this.i18n.t('common.deleteConfirm'))) {
      return;
    }
    this.adminApi.deleteFoodsharingStoreAutomation({ bezirkSlug: this.bezirkContext.currentSlug(), storeId: store.storeId }).subscribe({
      next: () => this.reloadActiveAutomationTab(),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  advertisementNumbers(store: FoodsharingStoreAutomation): number[] {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    return [1, 2, 3].filter((number) => data[`advertConfigured${number}`] || data[`advertStoreMessages${number}`] !== undefined || data[`advertTelegramMessages${number}`] !== undefined);
  }

  addAdvertisement(store: FoodsharingStoreAutomation): void {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const advertNumber = [1, 2, 3].find((number) => !data[`advertConfigured${number}`] && data[`advertStoreMessages${number}`] === undefined && data[`advertTelegramMessages${number}`] === undefined);
    if (!advertNumber) {
      return;
    }
    data[`advertConfigured${advertNumber}`] = true;
    data[`advertEnabled${advertNumber}`] = false;
    data[`advertDryRunEnabled${advertNumber}`] = true;
    data[`advertHoursBefore${advertNumber}`] = advertNumber === 1 ? 24 : advertNumber === 2 ? 12 : 3;
    data[`advertSendToStoreChat${advertNumber}`] = false;
    data[`advertSendToTelegram${advertNumber}`] = false;
    data[`advertStoreMessages${advertNumber}`] = '';
    data[`advertTelegramMessages${advertNumber}`] = '';
    data[`advertLateCancellationMessage${advertNumber}`] = 'Du hast gerade den Slot {{datetimeDe}} freigegeben. Du bist trotzdem verantwortlich für diesen Slot! Bitte sorge für Ersatz und notifiziere sowohl das Team als auch den Notfallchat.';
    data[`advertSendLateCancellationMessage${advertNumber}`] = true;
    data[`advertSendLatestAdvertisementAfterLateCancellation${advertNumber}`] = false;
    this.foodsharingStores.update((stores) => [...stores]);
  }

  addAdvertisementAutomation(): void {
    const storeId = this.selectedAdvertisementStoreId();
    const store = this.foodsharingStores().find((entry) => entry.storeId === storeId);
    if (!store) {
      return;
    }
    this.selectedAdvertisementStoreId.set(null);
    this.addAdvertisement(store);
  }

  requestAudit(): FoodsharingExtraAutomationAudit[] {
    return this.requestAutomationAudit();
  }

  addFoodsharingStore(): void {
    const storeId = this.selectedStoreId();
    const store = this.availableStores().find((entry) => entry.storeId === storeId);
    if (!store) {
      return;
    }
    this.adminApi.saveFoodsharingStoreAutomation({
      bezirkSlug: this.bezirkContext.currentSlug(),
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
        this.reloadActiveAutomationTab();
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
    this.adminApi.runFoodsharingAutomation({ bezirkSlug: this.bezirkContext.currentSlug(), foodsharingRunRequest: { dryRun: true } }).subscribe({
      next: (result) => {
        this.slotRunResult.set(result);
        this.loadFoodsharingAuditIfActive();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }


  saveCleaningRuleExemption(): void {
    this.adminApi.saveFoodsharingCleaningRuleExemption({
      bezirkSlug: this.bezirkContext.currentSlug(),
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
    this.adminApi.deleteFoodsharingCleaningRuleExemption({ bezirkSlug: this.bezirkContext.currentSlug(), exemptionId: exemption.id }).subscribe({
      next: () => this.loadCleaningRuleExemptions(),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  private loadFoodsharingStatus(): void {
    this.adminApi.getFoodsharingStatus().subscribe({
      next: (status) => {
        this.foodsharingStatus.set(status);
        if (status.connected) {
          this.loadActiveAutomationTab();
        } else {
          this.foodsharingStores.set([]);
          this.availableStores.set([]);
          this.foodsharingFuturePickupUsers.set([]);
          this.foodsharingAudit.set([]);
          this.requestAutomationAudit.set([]);
          this.advertisementAutomationAudit.set([]);
          this.telegramChatOptions.set([]);
        }
      },
      error: () => undefined
    });
  }

  private loadBezirkSettings(): void {
    this.adminApi.getAdminBezirk({ bezirkSlug: this.bezirkContext.currentSlug() }).subscribe({
      next: (bezirk) => this.cleaningStoreConfigured.set(bezirk.cleaningStoreId != null),
      error: () => this.cleaningStoreConfigured.set(null)
    });
  }

  private loadActiveAutomationTab(): void {
    if (!this.foodsharingStatus()?.connected) {
      return;
    }
    const tab = this.activeAutomationTab();
    switch (tab) {
      case 'applications':
        this.loadStoreAutomation({ loadRequestOverview: true, loadAdvertisementOverview: false });
        this.loadRequestAutomationAuditIfActive();
        break;
      case 'slots':
        this.loadStoreAutomation({ loadRequestOverview: false, loadAdvertisementOverview: false });
        this.loadCleaningRuleExemptions();
        this.loadFoodsharingAuditIfActive();
        break;
      case 'advertisements':
        this.loadStoreAutomation({ loadRequestOverview: false, loadAdvertisementOverview: true });
        this.loadAdvertisementAutomationAuditIfActive();
        this.loadTelegramChats();
        break;
      case 'statistics':
        this.loadFoodsharingFuturePickupUsers();
        break;
    }
  }

  private reloadActiveAutomationTab(): void {
    this.loadActiveAutomationTab();
  }

  private loadStoreAutomation(options: { loadRequestOverview: boolean; loadAdvertisementOverview: boolean }): void {
    this.adminApi.getFoodsharingStores({ bezirkSlug: this.bezirkContext.currentSlug() }).subscribe({
      next: (overview: FoodsharingStoreAutomationOverview) => {
        const stores: FoodsharingStoreAutomation[] = overview.automations
          .map((store) => ({ ...store, slotApprovalConfigured: true } as FoodsharingStoreAutomation & Record<string, unknown>));
        if (this.sessionService.hasPermission(UserPermission.CanUseAutomationRequestApproval)
            || this.sessionService.hasPermission(UserPermission.CanUseAutomationOpenSlotAdvertising)) {
          for (const availableStore of overview.availableStores) {
            if (!stores.some((store) => store.storeId === availableStore.storeId)) {
              stores.push({
                storeId: availableStore.storeId,
                storeName: availableStore.storeName,
                enabled: false,
                dryRunEnabled: true,
                gapRuleEnabled: false,
                minimumGapDays: 0,
                cleaningRuleEnabled: false,
                experienceRuleEnabled: false,
                editable: true
              });
            }
          }
        }
        this.foodsharingStores.set(stores);
        this.availableStores.set(overview.availableStores);
        if (options.loadRequestOverview) {
          this.loadRequestAutomationOverview();
        }
        if (options.loadAdvertisementOverview) {
          this.loadAdvertisementAutomationOverview();
        }
      },
      error: () => undefined
    });
  }

  private loadFoodsharingFuturePickupUsers(): void {
    this.adminApi.getFoodsharingFuturePickupUsers({ bezirkSlug: this.bezirkContext.currentSlug() }).subscribe({
      next: (users) => this.foodsharingFuturePickupUsers.set(users),
      error: () => undefined
    });
  }


  private loadRequestAutomationOverview(): void {
    this.adminApi.getFoodsharingRequestAutomationOverview().subscribe({
      next: (overview: FoodsharingRequestAutomationOverview) => {
        const stores = this.foodsharingStores().map((store) => ({ ...store }) as FoodsharingStoreAutomation & Record<string, unknown>);
        for (const requestAutomation of overview.requestAutomations || []) {
          const store = stores.find((entry) => entry.storeId === requestAutomation.storeId);
          if (store) {
            store['requestConfigured'] = true;
            store['requestEnabled'] = requestAutomation.enabled;
            store['requestDryRunEnabled'] = requestAutomation.dryRunEnabled;
            store['requestDistanceRuleEnabled'] = requestAutomation.distanceRuleEnabled;
            store['requestMaximumDistanceKm'] = requestAutomation.maximumDistanceKm;
          }
        }
        this.foodsharingStores.set(stores as FoodsharingStoreAutomation[]);
      },
      error: () => undefined
    });
  }

  private loadAdvertisementAutomationOverview(): void {
    this.adminApi.getFoodsharingOpenSlotAdvertisementOverview().subscribe({
      next: (overview: FoodsharingOpenSlotAdvertisementOverview) => {
        const stores = this.foodsharingStores().map((store) => ({ ...store }) as FoodsharingStoreAutomation & Record<string, unknown>);
        for (const advert of overview.advertisementAutomations || []) {
          const store = stores.find((entry) => entry.storeId === advert.storeId);
          if (store) {
            const number = advert.advertNumber;
            store[`advertConfigured${number}`] = true;
            store[`advertEnabled${number}`] = advert.enabled;
            store[`advertDryRunEnabled${number}`] = advert.dryRunEnabled;
            store[`advertHoursBefore${number}`] = advert.triggerHoursBefore;
            store[`advertSendToStoreChat${number}`] = advert.sendToStoreChat;
            store[`advertSendToTelegram${number}`] = advert.sendToTelegram;
            store[`advertTelegramChatId${number}`] = advert.telegramChatId;
            store[`advertStoreMessages${number}`] = (advert.storeMessages || []).join('\n---\n');
            store[`advertTelegramMessages${number}`] = (advert.telegramMessages || []).join('\n---\n');
            store[`advertLateCancellationMessage${number}`] = advert.lateCancellationMessage;
            store[`advertSendLateCancellationMessage${number}`] = advert.sendLateCancellationMessage;
            store[`advertSendLatestAdvertisementAfterLateCancellation${number}`] = advert.sendLatestAdvertisementAfterLateCancellation;
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

  private loadRequestAutomationAudit(): void {
    this.adminApi.getFoodsharingRequestAutomationAudit({ onlyMine: this.onlyMyDecisions() }).subscribe({
      next: (audit) => this.requestAutomationAudit.set(audit),
      error: () => undefined
    });
  }

  private loadAdvertisementAutomationAudit(): void {
    this.adminApi.getFoodsharingOpenSlotAdvertisementAudit({ onlyMine: this.onlyMyDecisions() }).subscribe({
      next: (audit) => this.advertisementAutomationAudit.set(audit),
      error: () => undefined
    });
  }

  private loadRequestAutomationAuditIfActive(): void {
    if (this.activeAutomationTab() === 'applications') {
      this.loadRequestAutomationAudit();
    }
  }

  private loadAdvertisementAutomationAuditIfActive(): void {
    if (this.activeAutomationTab() === 'advertisements') {
      this.loadAdvertisementAutomationAudit();
    }
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
    this.adminApi.getFoodsharingCleaningRuleExemptions({ bezirkSlug: this.bezirkContext.currentSlug() }).subscribe({
      next: (exemptions) => this.cleaningRuleExemptions.set(exemptions),
      error: () => undefined
    });
  }

  private loadFoodsharingAudit(): void {
    this.adminApi.getFoodsharingAutomationAudit({ bezirkSlug: this.bezirkContext.currentSlug(), onlyMine: this.onlyMyDecisions() }).subscribe({
      next: (audit) => this.foodsharingAudit.set(audit),
      error: () => undefined
    });
  }

  private loadFoodsharingAuditIfActive(): void {
    if (this.activeAutomationTab() === 'slots') {
      this.loadFoodsharingAudit();
    }
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

  protected normalizeStoreId(value: number | { value?: number } | null): number | null {
    if (typeof value === 'number') {
      return value;
    }
    return value?.value ?? null;
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}
