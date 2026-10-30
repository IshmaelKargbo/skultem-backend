package com.moriba.skultem.application.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;

import lombok.RequiredArgsConstructor;

/**
 * Small things every Library Attendance use case needs: the school's clock (Freetown time - see
 * SchoolTimeZone), the acting user's name for the "by whom" columns, and turning a domain refusal into
 * a RuleException, so it is reported as a 400 and logged as REJECTED rather than as a failure.
 */
@Component
@RequiredArgsConstructor
public class LibraryAttendanceSupport {

    private final UserRepository userRepo;

    public Instant now() {
        return Instant.now();
    }

    public LocalDate today() {
        return LocalDate.now(SchoolTimeZone.ZONE);
    }

    public LocalDateTime localNow() {
        return LocalDateTime.now(SchoolTimeZone.ZONE);
    }

    public String userName(String userId) {
        if (userId == null) {
            return null;
        }
        return userRepo.findById(userId).map(User::getName).orElse(null);
    }

    /** Runs a domain action; a refusal from the domain (IllegalState/IllegalArgument) becomes a RuleException. */
    public static <T> T rule(Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new RuleException(e.getMessage());
        }
    }

    public static void rule(Runnable action) {
        rule(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Saves a row the database keeps unique (one open visit per student, one session per timetable entry and
     * date). Losing a race to someone saving the same thing at the same moment becomes a refusal with
     * {@code message}, like AssignSubjectToTeacherUseCase does for its constraint.
     */
    public static void saveUnique(Runnable save, String message) {
        try {
            save.run();
        } catch (DataIntegrityViolationException e) {
            throw new RuleException(message);
        }
    }

    /** "JSS 2 A Science" - the class a student is in, as shown on registers and visit history. */
    public static String className(Enrollment enrollment) {
        StringBuilder name = new StringBuilder();
        if (enrollment.getClazz() != null && enrollment.getClazz().getName() != null) {
            name.append(enrollment.getClazz().getName());
        }
        if (enrollment.getSection() != null && enrollment.getSection().getName() != null
                && !enrollment.getSection().getName().isBlank()) {
            name.append(' ').append(enrollment.getSection().getName());
        }
        if (enrollment.getStream() != null && enrollment.getStream().getName() != null
                && !enrollment.getStream().getName().isBlank()) {
            name.append(' ').append(enrollment.getStream().getName());
        }
        return name.toString().trim();
    }
}
