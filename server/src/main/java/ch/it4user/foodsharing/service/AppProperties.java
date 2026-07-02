package ch.it4user.foodsharing.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Auth auth = new Auth();
    private final Frontend frontend = new Frontend();
    private final Foodsharing foodsharing = new Foodsharing();

    public Auth getAuth() {
        return auth;
    }

    public Frontend getFrontend() {
        return frontend;
    }

    public Foodsharing getFoodsharing() {
        return foodsharing;
    }

    public static class Auth {
        private long tokenValidityDays = 365;
        private long loginTokenValidityMinutes = 30;

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
        private String baseUrl = "https://foodsharing.de";
        private String adminUser = "";
        private String adminPassword = "";
        private String tokenEncryptionKey = "";
        private final Automation automation = new Automation();

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAdminUser() { return adminUser; }
        public void setAdminUser(String adminUser) { this.adminUser = adminUser; }
        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
        public String getTokenEncryptionKey() { return tokenEncryptionKey; }
        public void setTokenEncryptionKey(String tokenEncryptionKey) { this.tokenEncryptionKey = tokenEncryptionKey; }
        public Automation getAutomation() { return automation; }
    }

    public static class Automation {
        private boolean enabled = true;
        private boolean dryRun = true;
        private long cleaningStoreId = 0;
        private String pollInterval = "PT5M";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isDryRun() { return dryRun; }
        public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
        public long getCleaningStoreId() { return cleaningStoreId; }
        public void setCleaningStoreId(long cleaningStoreId) { this.cleaningStoreId = cleaningStoreId; }
        public String getPollInterval() { return pollInterval; }
        public void setPollInterval(String pollInterval) { this.pollInterval = pollInterval; }
    }
}
