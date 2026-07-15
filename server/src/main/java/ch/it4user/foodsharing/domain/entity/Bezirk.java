package ch.it4user.foodsharing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bezirke")
@Getter
@Setter
@NoArgsConstructor
public class Bezirk extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100, updatable = false)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "cleaning_store_id", unique = true)
    private Long cleaningStoreId;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
