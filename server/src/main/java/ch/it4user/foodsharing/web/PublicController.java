package ch.it4user.foodsharing.web;

import ch.it4user.foodsharing.openapi.api.PublicApi;
import ch.it4user.foodsharing.openapi.model.AvailableSlotListResponse;
import ch.it4user.foodsharing.openapi.model.BookSlotRequest;
import ch.it4user.foodsharing.openapi.model.BookingDetailResponse;
import ch.it4user.foodsharing.openapi.model.EinAbCategory;
import ch.it4user.foodsharing.openapi.model.NotificationSubscriptionRequest;
import ch.it4user.foodsharing.openapi.model.NotificationSubscriptionResponse;
import ch.it4user.foodsharing.openapi.model.NotificationUnsubscribeRequest;
import ch.it4user.foodsharing.openapi.model.TeacherResponse;
import ch.it4user.foodsharing.openapi.model.TeacherSignupRequest;
import ch.it4user.foodsharing.service.NotificationService;
import ch.it4user.foodsharing.service.PublicService;
import ch.it4user.foodsharing.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PublicController implements PublicApi {

    private final PublicService publicService;
    private final TeacherService teacherService;
    private final NotificationService notificationService;
    private final ApiModelMapper mapper;

    public PublicController(PublicService publicService,
                            TeacherService teacherService,
                            NotificationService notificationService,
                            ApiModelMapper mapper) {
        this.publicService = publicService;
        this.teacherService = teacherService;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<AvailableSlotListResponse> getAvailableSlots(String search,
                                                                       EinAbCategory category,
                                                                       Boolean visitFairteiler) {
        return ResponseEntity.ok(mapper.toAvailableSlotListResponse(
                publicService.findAvailableSlots(search, mapCategory(category), visitFairteiler)));
    }

    @Override
    public ResponseEntity<BookingDetailResponse> bookSlot(UUID slotId, BookSlotRequest bookSlotRequest) {
        return ResponseEntity.ok(mapper.toBookingDetailResponse(
                publicService.bookSlot(
                        slotId,
                        bookSlotRequest.getEmail(),
                        bookSlotRequest.getName(),
                        bookSlotRequest.getFoodsharingId(),
                        bookSlotRequest.getPhoneNumber())));
    }

    @Override
    public ResponseEntity<TeacherResponse> signupTeacher(TeacherSignupRequest teacherSignupRequest) {
        TeacherResponse response = mapper.toTeacherResponse(teacherService.signup(
                teacherSignupRequest.getEmail(),
                teacherSignupRequest.getFoodsharingId(),
                teacherSignupRequest.getName(),
                teacherSignupRequest.getIcalLink() == null ? null : teacherSignupRequest.getIcalLink().toString()
        ));
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<NotificationSubscriptionResponse> subscribeNotifications(
            NotificationSubscriptionRequest notificationSubscriptionRequest) {
        return ResponseEntity.ok(mapper.toNotificationResponse(
                notificationService.subscribe(notificationSubscriptionRequest.getEmail())));
    }

    @Override
    public ResponseEntity<NotificationSubscriptionResponse> unsubscribeNotifications(
            NotificationUnsubscribeRequest notificationUnsubscribeRequest) {
        return ResponseEntity.ok(mapper.toNotificationResponse(
                notificationService.unsubscribe(notificationUnsubscribeRequest.getToken())));
    }

    private ch.it4user.foodsharing.domain.enumtype.EinAbCategory mapCategory(EinAbCategory category) {
        return category == null ? null : ch.it4user.foodsharing.domain.enumtype.EinAbCategory.valueOf(category.getValue());
    }
}
