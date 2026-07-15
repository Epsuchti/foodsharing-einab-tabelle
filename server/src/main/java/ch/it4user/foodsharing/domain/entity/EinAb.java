package ch.it4user.foodsharing.domain.entity;

import ch.it4user.foodsharing.domain.enumtype.EinAbCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "einabs")
@Getter
@Setter
@NoArgsConstructor
public class EinAb extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bezirk_id", nullable = false)
    private Bezirk bezirk;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private EinAbCategory category;

    @Column(nullable = false)
    private Instant startDateTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private boolean visitFairteiler;

    @Column(nullable = false)
    private int slotCount;

    @Column
    private Integer minimumPickupCount;

    @Column(length = 500)
    private String location;

    @Column(nullable = false, length = 500)
    private String publicLocation;

    @Column(length = 4000)
    private String whatToBring;

    @Column(length = 4000)
    private String hint;
}
