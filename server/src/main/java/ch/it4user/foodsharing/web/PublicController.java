package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.PublicApi;
import ch.it4user.foodsharing.openapi.model.AvailableSlotListResponse;
import ch.it4user.foodsharing.openapi.model.BookSlotRequest;
import ch.it4user.foodsharing.openapi.model.BookingDetailResponse;
import ch.it4user.foodsharing.openapi.model.AuthResponse;
import ch.it4user.foodsharing.openapi.model.EinAbCategory;
import ch.it4user.foodsharing.openapi.model.Language;
import ch.it4user.foodsharing.openapi.model.NotificationSubscriptionRequest;
import ch.it4user.foodsharing.openapi.model.NotificationSubscriptionResponse;
import ch.it4user.foodsharing.openapi.model.NotificationUnsubscribeRequest;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.openapi.model.TeacherSignupRequest;
import ch.it4user.foodsharing.service.AuthService;
import ch.it4user.foodsharing.service.PublicService;
import ch.it4user.foodsharing.service.NewsletterService;
import ch.it4user.foodsharing.service.TeacherService;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PublicController implements PublicApi {

    private final PublicService publicService;
    private final TeacherService teacherService;
    private final NewsletterService newsletterService;
    private final AuthService authService;
    private final ApiModelMapper mapper;

    public PublicController(PublicService publicService,
                            TeacherService teacherService,
                            NewsletterService newsletterService,
                            AuthService authService,
                            ApiModelMapper mapper) {
        this.publicService = publicService;
        this.teacherService = teacherService;
        this.newsletterService = newsletterService;
        this.authService = authService;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<AvailableSlotListResponse> getAvailableSlots(
            String search,
            EinAbCategory category,
            Boolean visitFairteiler,
            Integer page,
            Integer size) {
        return ResponseEntity.ok(mapper.toAvailableSlotListResponse(
                publicService.findAvailableSlots(search, mapCategory(category), visitFairteiler, page == null ? 0 : page, size == null ? 20 : size)));
    }

    @Override
    public ResponseEntity<BookingDetailResponse> bookSlot(UUID slotId, BookSlotRequest bookSlotRequest) {
        return ResponseEntity.ok(mapper.toBookingDetailResponse(
                publicService.bookSlot(
                        slotId,
                        bookSlotRequest.getFoodsharingId(),
                        mapLanguage(bookSlotRequest.getLanguage()))));
    }


    @Override
    public ResponseEntity<BookingDetailResponse> confirmBooking(ch.it4user.foodsharing.openapi.model.VerifyTokenRequest verifyTokenRequest) {
        var slot = publicService.confirmBooking(verifyTokenRequest.getToken());
        var response = ResponseEntity.ok();
        Optional<AuthResponse> authResponse = authService.authenticateFoodsharingIdIfPossible(slot.getBookingUser().getFoodsharingId());
        authResponse.ifPresent(auth -> response
                .header("X-Auth-Token", auth.getAuthToken())
                .header("X-Auth-Expires-At", auth.getExpiresAt().toString())
                .header("X-Auth-Foodsharing-Id", auth.getFoodsharingId())
                .header("X-Auth-Permissions", String.join(",", auth.getPermissions().stream().map(Enum::name).toList()))
                .header("X-Auth-Display-Name", auth.getDisplayName() == null ? "" : auth.getDisplayName()));
        return response.body(mapper.toBookingDetailResponse(slot));
    }
    @Override
    public ResponseEntity<TeacherResponse> signupTeacher(TeacherSignupRequest teacherSignupRequest) {
        TeacherResponse response = mapper.toTeacherResponse(teacherService.signup(
                teacherSignupRequest.getFoodsharingId(),
                mapLanguage(teacherSignupRequest.getLanguage())
        ));
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<NotificationSubscriptionResponse> subscribeNotifications(@Valid NotificationSubscriptionRequest notificationSubscriptionRequest) {
        return ResponseEntity.ok(mapper.toNotificationSubscriptionResponse(
                newsletterService.subscribe(notificationSubscriptionRequest.getEmail(), mapLanguage(notificationSubscriptionRequest.getLanguage()))));
    }

    @Override
    public ResponseEntity<NotificationSubscriptionResponse> unsubscribeNotifications(NotificationUnsubscribeRequest notificationUnsubscribeRequest) {
        return ResponseEntity.ok(mapper.toNotificationSubscriptionResponse(
                newsletterService.unsubscribe(notificationUnsubscribeRequest.getToken())));
    }

    private ch.it4user.foodsharing.domain.enumtype.EinAbCategory mapCategory(EinAbCategory category) {
        return category == null ? null : ch.it4user.foodsharing.domain.enumtype.EinAbCategory.valueOf(category.getValue());
    }

    private ch.it4user.foodsharing.domain.enumtype.LanguageCode mapLanguage(Language language) {
        return ch.it4user.foodsharing.domain.enumtype.LanguageCode.fromCode(language == null ? "de" : language.getValue());
    }
}
