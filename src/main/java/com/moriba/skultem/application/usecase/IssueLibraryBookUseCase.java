package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.moriba.skultem.application.dto.LibraryLoanDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.LibraryMapper;
import com.moriba.skultem.domain.model.LibraryLoan;
import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;
import com.moriba.skultem.domain.repository.LibraryBookRepository;
import com.moriba.skultem.domain.model.LibrarySetting;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;
import com.moriba.skultem.domain.repository.LibrarySettingRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

/**
 * Lends one copy of a book to a student or a teacher. The book row is locked while this runs, so
 * two loans issued at the same moment can't both take the last copy. A borrower can't hold two
 * copies of the same book at once, and the school's {@link LibrarySetting} rules apply: a limit on
 * how many books they hold, and (when switched on) no new loan while one is overdue.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class IssueLibraryBookUseCase {
    private final LibraryBookRepository bookRepo;
    private final LibraryLoanRepository loanRepo;
    private final StudentRepository studentRepo;
    private final TeacherRepository teacherRepo;
    private final LibrarySettingRepository settingRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_BOOK_ISSUED")
    public LibraryLoanDTO execute(String schoolId, String bookId, BorrowerType borrowerType, String borrowerId,
            LocalDate dueDate) {
        var book = bookRepo.findByIdAndSchoolForUpdate(bookId, schoolId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (loanRepo.existsActiveByBookAndBorrower(bookId, borrowerId, schoolId)) {
            throw new RuleException("This borrower already has a copy of \"" + book.getTitle() + "\" on loan");
        }

        LocalDate today = LocalDate.now();

        var setting = settingRepo.findBySchool(schoolId).orElseGet(() -> LibrarySetting.defaults(schoolId));
        String blocked = setting.blockReason(borrowerType,
                loanRepo.countActiveByBorrower(borrowerId, schoolId),
                loanRepo.countOverdueByBorrower(borrowerId, schoolId, today));
        if (blocked != null) {
            throw new RuleException("Can't issue this book. The borrower " + Character.toLowerCase(blocked.charAt(0))
                    + blocked.substring(1));
        }

        LibraryLoan loan = switch (borrowerType) {
            case STUDENT -> {
                var student = studentRepo.findByIdAndSchoolId(borrowerId, schoolId)
                        .orElseThrow(() -> new NotFoundException("Student not found"));
                yield LibraryLoan.issueToStudent(schoolId, book, student, today, dueDate);
            }
            case TEACHER -> {
                var teacher = teacherRepo.findByIdAndSchoolId(borrowerId, schoolId)
                        .orElseThrow(() -> new NotFoundException("Teacher not found"));
                yield LibraryLoan.issueToTeacher(schoolId, book, teacher, today, dueDate);
            }
        };

        bookRepo.save(book);
        loanRepo.save(loan);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library book issued",
                book.getTitle() + " - " + loan.getBorrowerName(), null, loan.getId());

        return LibraryMapper.toDTO(loan, today);
    }
}
