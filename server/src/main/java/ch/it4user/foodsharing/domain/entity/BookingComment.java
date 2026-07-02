package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_comments")
@Getter
@Setter
@NoArgsConstructor
public class BookingComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_user_id", nullable = false)
    private User bookingUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false, length = 4000)
    private String comment;
}
