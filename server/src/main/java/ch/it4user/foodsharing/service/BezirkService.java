package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.Bezirk;
import ch.it4user.foodsharing.repository.BezirkRepository;
import ch.it4user.foodsharing.repository.FoodsharingStoreAutomationRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BezirkService {

    private final BezirkRepository bezirkRepository;
    private final FoodsharingStoreAutomationRepository storeAutomationRepository;

    public BezirkService(BezirkRepository bezirkRepository,
                         FoodsharingStoreAutomationRepository storeAutomationRepository) {
        this.bezirkRepository = bezirkRepository;
        this.storeAutomationRepository = storeAutomationRepository;
    }

    @Transactional(readOnly = true)
    public List<Bezirk> findAllActive() {
        return bezirkRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public Bezirk requireActive(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BEZIRK_NOT_FOUND);
        }
        return bezirkRepository.findBySlugAndActiveTrue(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.BEZIRK_NOT_FOUND));
    }

    @Transactional
    public Bezirk updateCleaningStoreId(String slug, Long cleaningStoreId) {
        if (cleaningStoreId != null && cleaningStoreId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED,
                    List.of("Cleaning store ID must be positive."));
        }
        Bezirk bezirk = requireActive(slug);
        if (cleaningStoreId != null
                && bezirkRepository.existsByCleaningStoreIdAndIdNot(cleaningStoreId, bezirk.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED,
                    List.of("Cleaning store ID is already assigned to another Bezirk."));
        }
        bezirk.setCleaningStoreId(cleaningStoreId);
        if (cleaningStoreId == null) {
            storeAutomationRepository.findAllByBezirkAndCleaningRuleEnabledTrue(bezirk)
                    .forEach(automation -> automation.setCleaningRuleEnabled(false));
        }
        return bezirkRepository.save(bezirk);
    }
}
