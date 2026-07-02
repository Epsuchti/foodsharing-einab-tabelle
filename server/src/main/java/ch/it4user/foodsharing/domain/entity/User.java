package ch.it4user.foodsharing.domain.entity;

import ch.it4user.foodsharing.domain.enumtype.LanguageCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String foodsharingId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String phoneNumber;

    @Column(length = 2000)
    private String icalLink;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name = "is_teacher", nullable = false)
    private boolean teacher = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LanguageCode preferredLanguage = LanguageCode.DE;
}
