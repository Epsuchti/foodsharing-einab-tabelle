package ch.it4user.foodsharing.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Auth auth = new Auth();
    private final Frontend frontend = new Frontend();
    private final Foodsharing foodsharing = new Foodsharing();
    private final Mail mail = new Mail();

    public Auth getAuth() {
        return auth;
    }

    public Frontend getFrontend() {
        return frontend;
    }

    @Valid
    public Foodsharing getFoodsharing() {
        return foodsharing;
    }

    public Mail getMail() {
        return mail;
    }

    public static class Auth {
        private long tokenValidityDays = 365;
        private long loginTokenValidityMinutes = 30;
        private long bookingConfirmationValidityMinutes = 60;

        public long getTokenValidityDays() {
            return tokenValidityDays;
        }

        public void setTokenValidityDays(long tokenValidityDays) {
            this.tokenValidityDays = tokenValidityDays;
        }

        public long getLoginTokenValidityMinutes() {
            return loginTokenValidityMinutes;
        }

        public void setLoginTokenValidityMinutes(long loginTokenValidityMinutes) {
            this.loginTokenValidityMinutes = loginTokenValidityMinutes;
        }

        public long getBookingConfirmationValidityMinutes() {
            return bookingConfirmationValidityMinutes;
        }

        public void setBookingConfirmationValidityMinutes(long bookingConfirmationValidityMinutes) {
            this.bookingConfirmationValidityMinutes = bookingConfirmationValidityMinutes;
        }

    }

    public static class Frontend {
        private String baseUrl = "http://localhost:4200";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Foodsharing {
        private String baseUrl = "https://foodsharing.network";
        @NotBlank
        private String adminUser = "";
        @NotBlank
        private String adminPassword = "";
        private String tokenEncryptionKey = "";
        private String phoneNumberApiBaseUrl = "https://www.vserverli.de/fs";
        private String phoneNumberApiToken = "";
        private final Automation automation = new Automation();

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAdminUser() { return adminUser; }
        public void setAdminUser(String adminUser) { this.adminUser = adminUser; }
        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
        public String getTokenEncryptionKey() { return tokenEncryptionKey; }
        public void setTokenEncryptionKey(String tokenEncryptionKey) { this.tokenEncryptionKey = tokenEncryptionKey; }
        public String getPhoneNumberApiBaseUrl() { return phoneNumberApiBaseUrl; }
        public void setPhoneNumberApiBaseUrl(String phoneNumberApiBaseUrl) { this.phoneNumberApiBaseUrl = phoneNumberApiBaseUrl; }
        public String getPhoneNumberApiToken() { return phoneNumberApiToken; }
        public void setPhoneNumberApiToken(String phoneNumberApiToken) { this.phoneNumberApiToken = phoneNumberApiToken; }
        public Automation getAutomation() { return automation; }
    }

    public static class Automation {
        private boolean enabled = true;
        private boolean dryRun = true;
        private int cleaningBackCheckMonths = 6;
        private int minimumFreeCleaningSlots = 4;
        private String pollInterval = "PT5M";
        private String futurePickupCacheTtl = "PT15M";
        private String storePickupCacheTtl = "PT1M";
        private String storeMembersCacheTtl = "PT1M";
        private int maxRequestApprovalsPerRun = 50;
        private int maxAdvertisementsPerRun = 50;
        private int auditRetentionDays = 7;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isDryRun() { return dryRun; }
        public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
        public int getCleaningBackCheckMonths() { return cleaningBackCheckMonths; }
        public void setCleaningBackCheckMonths(int cleaningBackCheckMonths) { this.cleaningBackCheckMonths = cleaningBackCheckMonths; }
        public int getMinimumFreeCleaningSlots() { return minimumFreeCleaningSlots; }
        public void setMinimumFreeCleaningSlots(int minimumFreeCleaningSlots) { this.minimumFreeCleaningSlots = minimumFreeCleaningSlots; }
        public String getPollInterval() { return pollInterval; }
        public void setPollInterval(String pollInterval) { this.pollInterval = pollInterval; }
        public String getFuturePickupCacheTtl() { return futurePickupCacheTtl; }
        public void setFuturePickupCacheTtl(String futurePickupCacheTtl) { this.futurePickupCacheTtl = futurePickupCacheTtl; }
        public String getStorePickupCacheTtl() { return storePickupCacheTtl; }
        public void setStorePickupCacheTtl(String storePickupCacheTtl) { this.storePickupCacheTtl = storePickupCacheTtl; }
        public String getStoreMembersCacheTtl() { return storeMembersCacheTtl; }
        public void setStoreMembersCacheTtl(String storeMembersCacheTtl) { this.storeMembersCacheTtl = storeMembersCacheTtl; }
        public int getMaxRequestApprovalsPerRun() { return maxRequestApprovalsPerRun; }
        public void setMaxRequestApprovalsPerRun(int maxRequestApprovalsPerRun) { this.maxRequestApprovalsPerRun = maxRequestApprovalsPerRun; }
        public int getMaxAdvertisementsPerRun() { return maxAdvertisementsPerRun; }
        public void setMaxAdvertisementsPerRun(int maxAdvertisementsPerRun) { this.maxAdvertisementsPerRun = maxAdvertisementsPerRun; }
        public int getAuditRetentionDays() { return auditRetentionDays; }
        public void setAuditRetentionDays(int auditRetentionDays) { this.auditRetentionDays = auditRetentionDays; }
    }

    public static class Mail {
        private String from = "noreply@foodsharing.network";

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }
}
