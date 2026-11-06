package com.moriba.skultem.application.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.domain.model.LibraryLoan.BorrowerType;
import com.moriba.skultem.domain.model.LibrarySetting;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.LibraryLoanRepository;
import com.moriba.skultem.domain.repository.LibrarySettingRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;

// Each management section (e.g. Primary, Secondary) can run its own library. A book's section decides which rules
// apply when it is lent: that section's own borrowing rules (or the school's, until it sets its own), loan limits
// counted only against that library's books, and only borrowers who belong to that section - a Primary book goes to
// Primary students, and to teachers who are whole-school or work in Primary.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryBorrowingRules {

    private final LibrarySettingRepository settingRepo;
    private final LibraryLoanRepository loanRepo;
    private final SchoolRepository schoolRepo;
    private final SchoolLevelRepository schoolLevelRepo;
    private final ManagementSectionRepository sectionRepo;
    private final StaffManagementSectionRepository staffSectionRepo;
    private final TeacherRepository teacherRepo;
    private final JdbcTemplate jdbc;

    /** The rules in force for a section's library: its own, else the school's, else the defaults. */
    public LibrarySetting settingFor(String schoolId, String sectionId) {
        if (sectionId != null) {
            var own = settingRepo.findBySchoolAndSection(schoolId, sectionId);
            if (own.isPresent()) {
                return own.get();
            }
        }
        return settingRepo.findBySchool(schoolId).orElseGet(() -> LibrarySetting.defaults(schoolId));
    }

    public long activeLoans(String schoolId, String borrowerId, String sectionId) {
        return sectionId == null ? loanRepo.countActiveByBorrower(borrowerId, schoolId)
                : loanRepo.countActiveByBorrowerInSection(borrowerId, schoolId, sectionId);
    }

    public long overdueLoans(String schoolId, String borrowerId, String sectionId, LocalDate today) {
        return sectionId == null ? loanRepo.countOverdueByBorrower(borrowerId, schoolId, today)
                : loanRepo.countOverdueByBorrowerInSection(borrowerId, schoolId, sectionId, today);
    }

    /** Why this borrower can't use this section's library, or null if they can. */
    public String outsideSectionReason(String schoolId, BorrowerType type, String borrowerId, String sectionId) {
        if (sectionId == null || !sectionBased(schoolId)) {
            return null;
        }
        String sectionName = sectionRepo.findBySchoolId(schoolId).stream()
                .filter(s -> s.getId().equals(sectionId))
                .map(s -> s.getName())
                .findFirst().orElse("this section");

        if (type == BorrowerType.STUDENT) {
            return studentInSection(schoolId, borrowerId, sectionId) ? null
                    : "Isn't a " + sectionName + " student, so can't borrow from the " + sectionName + " library.";
        }

        var teacher = teacherRepo.findByIdAndSchoolId(borrowerId, schoolId).orElse(null);
        if (teacher == null || teacher.getUser() == null) {
            return null;
        }
        var sections = staffSectionRepo.findBySchoolAndUserAndRole(schoolId, teacher.getUser().getId(), Role.TEACHER);
        boolean allowed = sections.isEmpty()
                || sections.stream().anyMatch(s -> sectionId.equals(s.getManagementSectionId()));
        return allowed ? null
                : "Doesn't work in " + sectionName + ", so can't borrow from the " + sectionName + " library.";
    }

    /** Everything that stops this loan, in the order the librarian should fix it, or null if it can go ahead. */
    public String blockReason(String schoolId, BorrowerType type, String borrowerId, String sectionId,
            LocalDate today) {
        String outside = outsideSectionReason(schoolId, type, borrowerId, sectionId);
        if (outside != null) {
            return outside;
        }
        return settingFor(schoolId, sectionId).blockReason(type, activeLoans(schoolId, borrowerId, sectionId),
                overdueLoans(schoolId, borrowerId, sectionId, today));
    }

    private boolean sectionBased(String schoolId) {
        return schoolRepo.findById(schoolId)
                .map(s -> s.getManagementModel() == ManagementModel.SECTION_BASED).orElse(false);
    }

    // Enrolled this academic year in a class at one of the section's levels.
    private boolean studentInSection(String schoolId, String studentId, String sectionId) {
        List<String> levels = schoolLevelRepo.findBySchoolId(schoolId).stream()
                .filter(l -> sectionId.equals(l.getManagementSectionId()))
                .map(l -> l.getLevel().name())
                .toList();
        if (levels.isEmpty()) {
            return false;
        }
        String sql = """
                SELECT count(*)
                FROM enrollments e
                JOIN classes c ON c.id = e.class_id
                JOIN academic_years ay ON ay.id = e.academic_year_id
                WHERE e.school_id = ? AND e.student_id = ? AND ay.active = TRUE AND c.level IN (%s)
                """.formatted(String.join(",", levels.stream().map(l -> "?").toList()));
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        args.add(studentId);
        args.addAll(levels);
        Long count = jdbc.queryForObject(sql, Long.class, args.toArray());
        return count != null && count > 0;
    }
}
