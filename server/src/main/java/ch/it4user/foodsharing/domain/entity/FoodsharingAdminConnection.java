package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "foodsharing_admin_connections", uniqueConstraints = @UniqueConstraint(name = "uk_foodsharing_admin_connections_admin", columnNames = "admin_user_id"))
@Getter
@Setter
@NoArgsConstructor
public class FoodsharingAdminConnection extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;

    @Column(length = 100)
    private String foodsharingUserId;

    @Column(nullable = false, length = 255)
    private String foodsharingEmail;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String sessionCookieCiphertext;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String csrfTokenCiphertext;

    @Column
    private Instant authenticatedAt;
}
