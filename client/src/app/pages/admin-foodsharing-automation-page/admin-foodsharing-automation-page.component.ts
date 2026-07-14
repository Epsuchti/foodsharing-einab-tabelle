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
  protected readonly requestAutomationAudit = signal<FoodsharingExtraAutomationAudit[]>([]);
  protected readonly advertisementAutomationAudit = signal<FoodsharingExtraAutomationAudit[]>([]);
  protected readonly foodsharingFuturePickupUsers = signal<FoodsharingFuturePickupUser[]>([]);
  protected readonly cleaningRuleExemptions = signal<FoodsharingCleaningRuleExemption[]>([]);
  protected readonly foodsharingEmail = signal('');
  protected readonly cleaningExemptionFoodsharingId = signal('');
  protected readonly cleaningExemptionReason = signal('');
  protected readonly slotRunResult = signal<FoodsharingRunResult | null>(null);
  protected readonly requestRunResult = signal<FoodsharingRunResult | null>(null);
  protected readonly advertisementRunResult = signal<FoodsharingRunResult | null>(null);
  protected readonly onlyMyAutomations = signal(true);
  protected readonly activeAutomationTab = signal('applications');
  protected readonly selectedStoreId = signal<number | null>(null);
  protected readonly selectedRequestStoreId = signal<number | null>(null);
  protected readonly selectedAdvertisementStoreId = signal<number | null>(null);
  protected readonly telegramChatOptions = signal<{ label: string; value: string }[]>([]);
  protected telegramBotToken = "";
  protected readonly visibleFoodsharingStores = computed(() => this.onlyMyAutomations()
    ? this.foodsharingStores().filter((store) => store.editable)
    : this.foodsharingStores());
  protected readonly slotApprovalStores = computed(() => this.visibleFoodsharingStores()
    .filter((store) => Boolean((store as FoodsharingStoreAutomation & Record<string, unknown>)['slotApprovalConfigured'])));
  protected readonly requestAutomationStores = computed(() => this.visibleFoodsharingStores()
    .filter((store) => Boolean((store as FoodsharingStoreAutomation & Record<string, unknown>)['requestConfigured'])));
  protected readonly requestDeclineMessagePreview = computed(() => this.buildRequestDeclineMessagePreview());
  protected readonly advertisementAutomationStores = computed(() => this.visibleFoodsharingStores()
    .filter((store) => this.advertisementNumbers(store).length > 0));
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
  protected readonly sessionService = inject(SessionService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.activeAutomationTab.set(this.sessionService.hasPermission(UserPermission.CanUseAutomationRequestApproval)
      ? 'applications'
      : this.sessionService.hasPermission(UserPermission.CanUseAutomationSlotApproval)
        ? 'slots'
        : this.sessionService.hasPermission(UserPermission.CanUseAutomationOpenSlotAdvertising) ? 'advertisements' : 'statistics');
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
        this.loadActiveAutomationTab();
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

  previewOpenSlotAdvertisement(store: FoodsharingStoreAutomation, advertNumber: number): string {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const firstTemplate = String(data[`advertMessages${advertNumber}`] || '').split('\n---\n').map((message) => message.trim()).filter(Boolean)[0];
    if (!firstTemplate) {
      return this.i18n.t('automation.addAdvertMessagePreview');
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

  private buildRequestDeclineMessagePreview(): string {
    const stores = this.requestAutomationStores() as Array<FoodsharingStoreAutomation & { requestDistanceRuleEnabled?: boolean; requestMaximumDistanceKm?: number }>;
    const store = stores.find((entry) => entry.requestDistanceRuleEnabled && Number(entry.requestMaximumDistanceKm ?? 0) > 0);
    if (!store) {
      return this.i18n.language() === 'en'
        ? 'Enable the distance rule to see the rejection message preview.'
        : this.i18n.language() === 'gws'
          ? `Aktiviere d'Distanzregle, zum e Ufahnde-Vorschau z gseh.`
          : 'Aktiviere die Distanzregel, um die Ablehnungsnachricht anzuzeigen.';
    }
    const maximumDistance = this.formatDistance(Number(store.requestMaximumDistanceKm ?? 0));
    if (this.i18n.language() === 'en') {
      return `The request is automatically declined because the distance of {{distanceInKm}} km exceeds the maximum of ${maximumDistance} km.`;
    }
    if (this.i18n.language() === 'gws') {
      return `D'Aafrag wird automäsch abglehnt, will d'Distanz vo {{distanceInKm}} km s'Maximum vo ${maximumDistance} km überschriitet.`;
    }
    return `Die Anfrage wird automatisch abgelehnt, weil die Entfernung von {{distanceInKm}} km das Maximum von ${maximumDistance} km überschreitet.`;
  }

  private formatDistance(distance: number): string {
    return Number.isInteger(distance) ? String(distance) : distance.toFixed(2).replace(/\.?0+$/, '');
  }

  saveOpenSlotAdvertisement(store: FoodsharingStoreAutomation, advertNumber: number): void {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const messages = String(data[`advertMessages${advertNumber}`] || '').split('\n---\n').map((message) => message.trim()).filter(Boolean);
    const sendToStoreChat = Boolean(data[`advertSendToStoreChat${advertNumber}`]);
    const sendToTelegram = Boolean(data[`advertSendToTelegram${advertNumber}`]);
    const telegramChatId = String(data[`advertTelegramChatId${advertNumber}`] || '').trim();
    if (!sendToStoreChat && !sendToTelegram) {
      this.toastError(this.i18n.t('automation.selectDestination'));
      return;
    }
    if (sendToTelegram && !telegramChatId) {
      this.toastError(this.i18n.t('automation.selectTelegramChat'));
      return;
    }
    if (messages.length === 0) {
      this.toastError(this.i18n.t('automation.addAdvertMessage'));
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


  sendTelegramTestMessage(store: FoodsharingStoreAutomation, advertNumber: number): void {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const chatId = String(data[`advertTelegramChatId${advertNumber}`] || '').trim();
    if (!chatId) {
      this.toastError(this.i18n.t('automation.selectTelegramChat'));
      return;
    }
    this.adminApi.sendFoodsharingTelegramTestMessage({
      telegramTestMessageRequest: {
        chatId,
        message: this.previewOpenSlotAdvertisement(store, advertNumber)
      }
    }).subscribe({
      next: () => this.messageService.add({ severity: 'success', summary: this.i18n.t('automation.telegramTestSent') }),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  saveFoodsharingStore(store: FoodsharingStoreAutomation): void {
    this.adminApi.saveFoodsharingStoreAutomation({
      storeId: store.storeId,
      foodsharingStoreAutomationRequest: store
    }).subscribe({
      next: () => this.reloadActiveAutomationTab(),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  advertisementNumbers(store: FoodsharingStoreAutomation): number[] {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    return [1, 2, 3].filter((number) => data[`advertConfigured${number}`] || data[`advertMessages${number}`] !== undefined);
  }

  addAdvertisement(store: FoodsharingStoreAutomation): void {
    const data = store as FoodsharingStoreAutomation & Record<string, unknown>;
    const advertNumber = [1, 2, 3].find((number) => !data[`advertConfigured${number}`] && data[`advertMessages${number}`] === undefined);
    if (!advertNumber) {
      return;
    }
    data[`advertConfigured${advertNumber}`] = true;
    data[`advertEnabled${advertNumber}`] = false;
    data[`advertHoursBefore${advertNumber}`] = advertNumber === 1 ? 24 : advertNumber === 2 ? 12 : 3;
    data[`advertSendToStoreChat${advertNumber}`] = false;
    data[`advertSendToTelegram${advertNumber}`] = false;
    data[`advertMessages${advertNumber}`] = '';
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

  advertisementAudit(): FoodsharingExtraAutomationAudit[] {
    return this.advertisementAutomationAudit();
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
    this.adminApi.runFoodsharingAutomation({ foodsharingRunRequest: { dryRun: true } }).subscribe({
      next: (result) => {
        this.slotRunResult.set(result);
        this.loadFoodsharingAuditIfActive();
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
    this.adminApi.getFoodsharingStores().subscribe({
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
    this.adminApi.getFoodsharingFuturePickupUsers().subscribe({
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

  private loadRequestAutomationAudit(): void {
    if (!this.sessionService.hasPermission(UserPermission.CanSeeAllAutomationDecisions)) {
      this.requestAutomationAudit.set([]);
      return;
    }
    this.adminApi.getFoodsharingRequestAutomationAudit().subscribe({
      next: (audit) => this.requestAutomationAudit.set(audit),
      error: () => undefined
    });
  }

  private loadAdvertisementAutomationAudit(): void {
    if (!this.sessionService.hasPermission(UserPermission.CanSeeAllAutomationDecisions)) {
      this.advertisementAutomationAudit.set([]);
      return;
    }
    this.adminApi.getFoodsharingOpenSlotAdvertisementAudit().subscribe({
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
