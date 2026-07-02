package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foodsharing_api_sessions")
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingApiSession extends BaseEntity {

    @Column(columnDefinition = "TEXT")
    private String sessionCookieCiphertext;

    @Column(columnDefinition = "TEXT")
    private String csrfTokenCiphertext;

    @Column
    private Instant authenticatedAt;
}
