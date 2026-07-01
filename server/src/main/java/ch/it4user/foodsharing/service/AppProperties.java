package ch.it4user.foodsharing.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Auth auth = new Auth();
    private final Frontend frontend = new Frontend();
    private final Mail mail = new Mail();

    public Auth getAuth() {
        return auth;
    }

    public Frontend getFrontend() {
        return frontend;
    }

    public Mail getMail() {
        return mail;
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

    public static class Mail {
        private String from = "no-reply@foodsharing.local";

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }
}
