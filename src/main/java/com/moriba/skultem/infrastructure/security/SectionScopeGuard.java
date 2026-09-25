package com.moriba.skultem.infrastructure.security;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;

/**
 * Single-record management-section checks for {@code @PreAuthorize}, e.g.
 * {@code "... and @sectionScope.classSession(#school, #id)"}. Whole-school callers pass straight
 * through without a lookup. A record that doesn't exist passes too, so the use case still answers
 * with its usual 404 rather than this turning it into a 403.
 */
@Service("sectionScope")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectionScopeGuard {

    private final SectionScopeService sectionScopeService;
    private final ClassRepository classRepo;
    private final ClassSessionRepository classSessionRepo;
    private final StudentRepository studentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final FeeStructureRepository feeStructureRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    private final ClassMasterRepository classMasterRepo;
    private final AssessmentApprovalRequestRepository approvalRequestRepo;
    private final ReportCardRepository reportCardRepo;
    private final PaymentRepository paymentRepo;
    private final TeacherRepository teacherRepo;
    private final ParentRepository parentRepo;
    private final StaffManagementSectionRepository staffSectionRepo;

    public boolean wholeSchool() {
        return sectionScopeService.current().wholeSchool();
    }

    // A management section's own settings (branding): whole-school callers may edit any, a
    // section-limited one only a section they're limited to.
    public boolean managementSection(String sectionId) {
        var scope = sectionScopeService.current();
        return scope.wholeSchool() || (sectionId != null && scope.sectionIds().contains(sectionId));
    }

    public boolean level(String level) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool()) {
            return true;
        }
        try {
            return level != null && scope.allows(Level.valueOf(level.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean clazz(String schoolId, String classId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || classId == null) {
            return true;
        }
        return classRepo.findByIdAndSchool(classId, schoolId).map(c -> scope.allows(c.getLevel())).orElse(true);
    }

    // For school-wide lists that take an optional class filter: a section-limited caller must give
    // one, inside their sections - that's what keeps the list from spanning every section.
    public boolean requiredClass(String schoolId, String classId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool()) {
            return true;
        }
        return classId != null && !classId.isBlank() && classRepo.findByIdAndSchool(classId, schoolId)
                .map(c -> scope.allows(c.getLevel())).orElse(false);
    }

    public boolean classes(String schoolId, List<String> classIds) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || classIds == null) {
            return true;
        }
        return classIds.stream().allMatch(id -> clazz(schoolId, id));
    }

    // Streams belong to streamed levels (SSS) - a caller whose sections cover none of those has no
    // business with any stream.
    public boolean stream(String schoolId, String streamId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || streamId == null) {
            return true;
        }
        return scope.levels().stream().anyMatch(Level::isStreamed);
    }

    public boolean classSession(String schoolId, String sessionId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || sessionId == null) {
            return true;
        }
        return classSessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .map(s -> scope.allows(s.getClazz().getLevel())).orElse(true);
    }

    // A teacher's subject in one class session (grading, submitting, re-opening).
    public boolean teacherSubject(String schoolId, String teacherSubjectId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || teacherSubjectId == null) {
            return true;
        }
        return teacherSubjectRepo.findByIdAndSchoolId(teacherSubjectId, schoolId)
                .map(t -> scope.allows(t.getSession().getClazz().getLevel())).orElse(true);
    }

    public boolean classMaster(String schoolId, String classMasterId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || classMasterId == null) {
            return true;
        }
        return classMasterRepo.findByIdAndSchoolId(classMasterId, schoolId)
                .map(m -> scope.allows(m.getSession().getClazz().getLevel())).orElse(true);
    }

    public boolean approvalRequest(String schoolId, String approvalRequestId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || approvalRequestId == null) {
            return true;
        }
        return approvalRequestRepo.findByIdAndSchoolId(approvalRequestId, schoolId)
                .map(r -> scope.allows(r.getTeacherSubject().getSession().getClazz().getLevel())).orElse(true);
    }

    // A report card records the class (Clazz) it was generated for.
    public boolean reportCard(String schoolId, String reportCardId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || reportCardId == null) {
            return true;
        }
        return reportCardRepo.findByIdAndSchoolId(reportCardId, schoolId)
                .map(r -> clazz(schoolId, r.getClassId())).orElse(true);
    }

    public boolean enrollment(String schoolId, String enrollmentId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || enrollmentId == null) {
            return true;
        }
        return enrollmentRepo.findByIdAndSchoolId(enrollmentId, schoolId)
                .map(e -> scope.allows(e.getClazz().getLevel())).orElse(true);
    }

    // A student belongs to the level of their most recent enrollment (their current class). One
    // who has never been placed in a class belongs to no section, so only whole-school staff see them.
    public boolean student(String schoolId, String studentId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || studentId == null) {
            return true;
        }
        Optional<Student> student = studentRepo.findByIdAndSchoolId(studentId, schoolId);
        if (student.isEmpty()) {
            return true;
        }
        return scope.allows(studentLevel(schoolId, student.get()));
    }

    // Every one of these students must be inside the caller's sections - e.g. enrolling students into
    // a class mustn't let a JSS admin pull in students from SSS.
    public boolean students(String schoolId, List<String> studentIds) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || studentIds == null) {
            return true;
        }
        return studentIds.stream().allMatch(id -> student(schoolId, id));
    }

    // Reading a fee: a class fee follows its class's level; a fee with no class (school-wide, or for
    // a hand-picked student list) applies across sections, so any accountant may see it.
    public boolean feeStructure(String schoolId, String feeId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || feeId == null) {
            return true;
        }
        return feeStructureRepo.findByIdAndSchoolId(feeId, schoolId)
                .map(f -> f.getClazz() == null || scope.allows(f.getClazz().getLevel())).orElse(true);
    }

    // Changing a fee (edit, delete, assign): only a class fee inside the caller's sections - a fee
    // with no class reaches students in other sections, so only whole-school staff can change it.
    public boolean manageFeeStructure(String schoolId, String feeId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || feeId == null) {
            return true;
        }
        return feeStructureRepo.findByIdAndSchoolId(feeId, schoolId)
                .map(f -> f.getClazz() != null && scope.allows(f.getClazz().getLevel())).orElse(true);
    }

    // Opening/editing a parent: whole-school staff always; a section-limited caller only when the parent
    // has a child in their section (or no children yet) - the same rule as the parents list.
    public boolean parent(String schoolId, String parentId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || parentId == null) {
            return true;
        }
        return parentRepo.findByIdAndSchoolId(parentId, schoolId)
                .map(p -> parentRepo.visibleInLevels(parentId, schoolId, scope.levels())).orElse(true);
    }

    // Opening/editing/deactivating a teacher: only one limited to sections inside the caller's own - a
    // whole-school teacher (or one in another section) is for whole-school staff only. The list is
    // filtered the same way (TeacherService#search); adding a teacher is section-neutral.
    public boolean teacher(String schoolId, String teacherId) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || teacherId == null) {
            return true;
        }
        return teacherRepo.findByIdAndSchoolId(teacherId, schoolId).map(t -> {
            var sections = staffSectionRepo.findBySchoolAndUserAndRole(schoolId, t.getUser().getId(), Role.TEACHER)
                    .stream().map(StaffManagementSection::getManagementSectionId).toList();
            return !sections.isEmpty() && scope.sectionIds().containsAll(sections);
        }).orElse(true);
    }

    // Creating fees: section-limited staff can only create CLASS fees for classes in their sections.
    public boolean createFee(String schoolId, String type, List<String> classIds) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool()) {
            return true;
        }
        return "CLASS".equals(type) && classIds != null && !classIds.isEmpty()
                && classIds.stream().allMatch(id -> id != null && classRepo.findByIdAndSchool(id, schoolId)
                        .map(c -> scope.allows(c.getLevel())).orElse(false));
    }

    // Recording a payment: the student must be in scope and every fee paid must be one they can see.
    public boolean payment(String schoolId, String studentId, List<String> feeIds) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool()) {
            return true;
        }
        return student(schoolId, studentId)
                && (feeIds == null || feeIds.stream().allMatch(id -> feeStructure(schoolId, id)));
    }

    // A receipt covers one student's payments (one reference number).
    public boolean receipt(String schoolId, String referenceNo) {
        var scope = sectionScopeService.current();
        if (scope.wholeSchool() || referenceNo == null) {
            return true;
        }
        return paymentRepo.findAllByReferenceNoAndSchoolId(referenceNo, schoolId).stream()
                .allMatch(p -> p.getStudent() != null && student(schoolId, p.getStudent().getId()));
    }

    public Level studentLevel(String schoolId, Student student) {
        return enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(student.getId(), schoolId)
                .map(Enrollment::getClazz)
                .map(c -> c.getLevel())
                .orElseGet(() -> student.getSession() != null && student.getSession().getClazz() != null
                        ? student.getSession().getClazz().getLevel()
                        : null);
    }
}
