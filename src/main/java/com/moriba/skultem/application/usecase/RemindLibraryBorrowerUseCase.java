package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LibraryLoanDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.LibraryMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LibraryLoan;
import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;
import com.moriba.skultem.domain.model.Notification.Type;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Priority;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Sends an in-app reminder about a book that hasn't come back: to the teacher who borrowed it, or
 * to every parent linked to the student who did. Records when the reminder went out, so the
 * overdue list can show it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RemindLibraryBorrowerUseCase {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final LibraryLoanRepository loanRepo;
    private final TeacherRepository teacherRepo;
    private final StudentParentRepository studentParentRepo;
    private final CreateNotificationUseCase createNotificationUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_BORROWER_REMINDED")
    public LibraryLoanDTO execute(String schoolId, String loanId) {
        var loan = loanRepo.findByIdAndSchool(loanId, schoolId)
                .orElseThrow(() -> new NotFoundException("Loan not found"));

        if (loan.isReturned()) {
            throw new RuleException("This book has already been returned");
        }

        List<User> recipients = recipients(loan, schoolId);
        if (recipients.isEmpty()) {
            throw new RuleException(loan.getBorrowerType() == BorrowerType.TEACHER
                    ? "This teacher has no account to send a reminder to"
                    : "This student has no parent account to send a reminder to");
        }

        LocalDate today = LocalDate.now();
        String title = loan.isOverdue(today) ? "Library book overdue" : "Library book reminder";
        String message = message(loan, today);
        Map<String, String> meta = Map.of(
                "loan_id", loan.getId(),
                "book_id", loan.getBook().getId(),
                "book_title", loan.getBook().getTitle(),
                "borrower_name", loan.getBorrowerName(),
                "due_date", loan.getDueDate().toString());

        for (User user : recipients) {
            createNotificationUseCase.execute(schoolId, user, Type.REMINDER, title, message, meta,
                    loan.isOverdue(today) ? Priority.HIGH : Priority.NORMAL);
        }

        loan.markNotified();
        loanRepo.save(loan);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library reminder sent",
                loan.getBook().getTitle() + " - " + loan.getBorrowerName(), null, loan.getId());

        return LibraryMapper.toDTO(loan, today);
    }

    private List<User> recipients(LibraryLoan loan, String schoolId) {
        if (loan.getBorrowerType() == BorrowerType.TEACHER) {
            return teacherRepo.findByIdAndSchoolId(loan.getTeacherId(), schoolId)
                    .map(teacher -> teacher.getUser())
                    .filter(user -> user != null)
                    .map(List::of)
                    .orElse(List.of());
        }

        Set<String> seen = new HashSet<>();
        List<User> users = new ArrayList<>();
        for (var link : studentParentRepo.findAllByStudentAndSchool(loan.getStudentId(), schoolId)) {
            var parent = link.getParent();
            if (parent != null && parent.getUser() != null && seen.add(parent.getUser().getId())) {
                users.add(parent.getUser());
            }
        }
        return users;
    }

    private static String message(LibraryLoan loan, LocalDate today) {
        String book = "\"" + loan.getBook().getTitle() + "\"";
        String due = loan.getDueDate().format(DATE);

        if (loan.getBorrowerType() == BorrowerType.TEACHER) {
            return loan.isOverdue(today)
                    ? "The library book " + book + " was due back on " + due + ". Please return it as soon as possible."
                    : "A reminder that the library book " + book + " is due back on " + due + ".";
        }

        return loan.isOverdue(today)
                ? loan.getBorrowerName() + " still has the library book " + book + ", which was due back on " + due
                        + ". Please help them return it as soon as possible."
                : "A reminder that " + loan.getBorrowerName() + " needs to return the library book " + book
                        + " by " + due + ".";
    }
}
