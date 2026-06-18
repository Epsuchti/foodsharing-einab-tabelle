package ch.it4user.foodsharing.service;

import ch.it4user.foodsharing.domain.entity.BookingUser;
import ch.it4user.foodsharing.domain.entity.EinAb;
import ch.it4user.foodsharing.domain.entity.Slot;
import ch.it4user.foodsharing.domain.entity.Teacher;
import ch.it4user.foodsharing.domain.enumtype.SlotStatus;
import ch.it4user.foodsharing.repository.SlotRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final TeacherService teacherService;
    private final BookingUserService bookingUserService;
    private final SlotRepository slotRepository;

    public AdminService(TeacherService teacherService,
                        BookingUserService bookingUserService,
                        SlotRepository slotRepository) {
        this.teacherService = teacherService;
        this.bookingUserService = bookingUserService;
        this.slotRepository = slotRepository;
    }

    public List<Teacher> getTeachers() {
        return teacherService.findAllTeachers();
    }

    public Teacher setTeacherActive(java.util.UUID teacherId, boolean active) {
        return teacherService.setTeacherActive(teacherId, active);
    }

    public List<EinAb> getEinAbs() {
        return teacherService.findAllEinAbs();
    }

    public List<Slot> getBookings() {
        return slotRepository.findAllByStatusInOrderByEinAbStartDateTimeAsc(Set.of(SlotStatus.BOOKED, SlotStatus.DONE));
    }

    public List<BookingUser> getUsers() {
        return bookingUserService.findAll();
    }
}
