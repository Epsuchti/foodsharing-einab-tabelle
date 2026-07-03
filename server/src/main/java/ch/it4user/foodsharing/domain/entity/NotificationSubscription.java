package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class NotificationSubscription extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LanguageCode language = LanguageCode.DE;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, unique = true, length = 128)
    private String unsubscribeToken;
}
