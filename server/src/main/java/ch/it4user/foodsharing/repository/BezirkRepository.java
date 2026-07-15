package ch.it4user.foodsharing.repository;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface BezirkRepository extends JpaRepository<Bezirk, UUID> {

    Optional<Bezirk> findBySlugAndActiveTrue(String slug);

    List<Bezirk> findAllByActiveTrueOrderBySortOrderAscNameAsc();

    boolean existsByCleaningStoreIdAndIdNot(Long cleaningStoreId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Bezirk> findFirstByOrderBySortOrderAscIdAsc();
}
