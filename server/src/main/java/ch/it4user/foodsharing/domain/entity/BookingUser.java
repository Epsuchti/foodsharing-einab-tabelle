package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_users")
@Getter
@Setter
@NoArgsConstructor
public class BookingUser extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String foodsharingId;

    @Column(nullable = false, length = 50)
    private String phoneNumber;

}
