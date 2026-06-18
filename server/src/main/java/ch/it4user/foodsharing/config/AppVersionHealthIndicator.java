package ch.it4user.foodsharing.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AppVersionHealthIndicator implements HealthIndicator {

    private final AppVersionResolver appVersionResolver;

    public AppVersionHealthIndicator(AppVersionResolver appVersionResolver) {
        this.appVersionResolver = appVersionResolver;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("version", appVersionResolver.resolve())
                .build();
    }
}
