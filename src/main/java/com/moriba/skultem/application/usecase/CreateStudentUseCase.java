package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.application.dto.ClassSubjectResponse;
import com.moriba.skultem.application.dto.OptionGroupResponse;
import com.moriba.skultem.application.dto.ParentRequest;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.dto.StudentParentRequest;
import com.moriba.skultem.application.dto.StudentRecord;
import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.application.mapper.SubjectMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.EnrollmentSubject;
import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.model.House;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.StreamSubject;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.model.StudentLedgerEntry.Direction;
import com.moriba.skultem.domain.model.StudentLedgerEntry.TransactionType;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.domain.repository.HouseRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.bucket.SupabaseStorageService;
import com.moriba.skultem.infrastructure.mail.MailService;
import com.moriba.skultem.utils.Generate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateStudentUseCase {
    private final StudentRepository repo;
    private final SchoolRepository schoolRepo;
    private final ClassSessionRepository classSessionRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final StreamSubjectRepository streamSubjectRepo;
    private final SubjectRepository subjectRepo;
    private final EnrollmentSubjectRepository enrollmentSubjectRepo;
    private final FeeStructureRepository feeStructureRepo;
    private final SupabaseStorageService storageService;
    private final ParentRepository parentRepo;
    private final HouseRepository houseRepo;
    private final StudentParentRepository studentParentRepo;
    private final MailService mailService;
    private final CreateParentUseCase createParentUseCase;
    private final StudentFeeRepository studentFeeRepo;
    private final EnrollmentCreationService enrollmentCreationService;
    private final CreateStudentLedgerUsercase createStudentLedgerUsercase;
    private final ProvisionStudentAssessmentsUseCase provisionStudentAssessmentsUseCase;
    private final CreateStudentParentUseCase createStudentParentUseCase;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_CREATED")
    public StudentDTO execute(StudentRecord param) {
        List<String> selectedOptionIds = param.selectedOptionIds() == null
                ? List.of()
                : param.selectedOptionIds().stream()
                        .filter(id -> id != null && !id.isBlank())
                        .distinct()
                        .toList();

        School school = schoolRepo.findById(param.schoolId())
                .orElseThrow(() -> new NotFoundException("School not found"));
        House house = null;

        if (param.house() != null) {
            house = houseRepo.findByIdAndSchool(param.house(), param.schoolId())
                    .orElseThrow(() -> new NotFoundException("House not found"));
        }

        Parent parent = applyParent(param.parentId(), param.parent());

        var session = classSessionRepo
                .findByIdAndSchoolId(param.classId(), param.schoolId())
                .orElseThrow(() -> new NotFoundException("Class not found"));

        String admissionNumber = param.admissionNumber().trim().toUpperCase();
        if (repo.existsByAdmissionNumberAndSchoolId(admissionNumber, param.schoolId())) {
            throw new RuleException("Admission number already exists");
        }

        Student student = Student.create(param.schoolId(), "", param.admissionNumber(), param.admissionDate(),
                param.givenNames(), param.familyName(), param.family(), session, param.lastClass(), param.gender(),
                parent, param.dateOfBirth(), param.enrollmentType(), param.previousSchool(), house, param.nationality(),
                param.religion(), param.city(), param.street());
        repo.save(student);
        if (!studentParentRepo.existByStudentIdAndParentIdAndSchoolId(student.getId(), parent.getId(),
                school.getId())) {
            var payload = new StudentParentRequest(school.getId(), parent.getId(), student.getId(),
                    param.relationship());
            createStudentParentUseCase.execute(payload);
        }

        Enrollment enrollment = enrollStudent(student, session);
        enrolledSubjects(enrollment, selectedOptionIds);
        provisionStudentAssessmentsUseCase.execute(enrollment);
        applyFees(enrollment);

        String photoUrl = null;
        if (param.photo() != null && !param.photo().isEmpty()) {
            try {
                photoUrl = uploadPhoto(param.photo(), admissionNumber, param.schoolId());
            } catch (Exception e) {
                System.err.println("Failed to upload photo: " + e.getMessage());
                throw new RuntimeException("Failed to upload photo", e);
            }
        }
        student.setProfile(photoUrl);
        repo.save(student);

        if (param.parentId().isBlank())
            sendWelcomeEmail(school, student);
        else
            sendLinkEmail(school, student);

        logActivityUseCase.log(
                param.schoolId(),
                ActivityType.STUDENT,
                "New student enrolled",
                student.getGivenNames() + " " + student.getFamilyName() + " - " + enrollment.getClazz().getName(),
                null,
                student.getId());

        return StudentMapper.toDTO(student, enrollment);
    }

    private Enrollment enrollStudent(Student student, ClassSession session) {
        Enrollment enrollment = enrollmentCreationService.create(
                student.getSchoolId(),
                student,
                session.getClazz(),
                session.getSection(),
                session.getAcademicYear(),
                session.getStream());
        return enrollment;
    }

    // apply parent details if parentId is provided, otherwise create new parent if
    // parent details are provided
    private Parent applyParent(String parentId, ParentRequest parent) {
        if (parentId != null && !parentId.isBlank()) {
            return parentRepo.findByIdAndSchoolId(parentId,
                    parent != null ? parent.schoolId() : null)
                    .orElseThrow(() -> new NotFoundException("Parent not found"));
        }

        if (parent == null) {
            return null;
        }

        return createParentUseCase.create(parent);
    }

    private void applyFees(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        AcademicYear academicYear = enrollment.getAcademicYear();
        Clazz clazz = enrollment.getClazz();
        String schoolId = enrollment.getSchoolId();

        List<FeeStructure> fees = feeStructureRepo.findApplicableFees(schoolId, academicYear.getId(), clazz.getId());

        int assignedCount = 0;
        BigDecimal totalAssignedAmount = BigDecimal.ZERO;

        for (FeeStructure fee : fees) {
            if (studentFeeRepo.existsBySchoolAndEnrollmentAndStudentAndFee(schoolId, enrollment.getId(),
                    student.getId(), fee.getId()))
                continue;

            StudentFee studentFee = StudentFee.create(schoolId, enrollment, student, fee, null);
            studentFeeRepo.save(studentFee);

            String description = Generate.generateLedgerDescription(
                    TransactionType.FEE_ASSINMENT,
                    fee.getTerm().getName(),
                    fee.getCategory().getName(),
                    student.getGivenNames(),
                    student.getFamilyName(),
                    student.getAdmissionNumber(),
                    fee.getAmount());

            createStudentLedgerUsercase.createEntry(
                    schoolId,
                    academicYear.getId(),
                    student.getId(),
                    fee.getTerm().getId(),
                    TransactionType.FEE_ASSINMENT,
                    Direction.DEBIT,
                    fee.getAmount(),
                    fee.getId(),
                    description,
                    Instant.now());

            assignedCount += 1;
            totalAssignedAmount = totalAssignedAmount.add(fee.getAmount());
        }

        if (assignedCount > 0) {
            String meta = "assignedCount=" + assignedCount + ";totalAmount=" + totalAssignedAmount;
            logActivityUseCase.log(
                    schoolId,
                    ActivityType.FEES,
                    "Fees assigned to student",
                    student.getGivenNames() + " " + student.getFamilyName(),
                    meta,
                    student.getId());
        }
    }

    private void enrolledSubjects(Enrollment enrollment, List<String> selectedOptionIds) {
        var curriculum = getSubjectCurriculum(enrollment);

        for (SubjectDTO core : curriculum.core()) {
            var subject = subjectRepo.findByIdAndSchoolId(core.id(), enrollment.getSchoolId())
                    .orElseThrow(() -> new NotFoundException("Subject not found"));
            var id = rg.generate("ENROLLMENT_SUBJECT", "ERM");
            var record = EnrollmentSubject.create(id, enrollment.getSchoolId(), enrollment, subject,
                    enrollment.getStudent());
            enrollmentSubjectRepo.save(record);
        }

        Map<String, List<String>> selectionsByGroup = new HashMap<>();
        for (String optionId : selectedOptionIds) {
            boolean found = false;
            for (OptionGroupResponse group : curriculum.options()) {
                if (group.list().stream().anyMatch(s -> s.id().equals(optionId))) {
                    selectionsByGroup.computeIfAbsent(group.groupId(), k -> new ArrayList<>()).add(optionId);
                    found = true;
                    break;
                }
            }
            if (!found)
                throw new RuleException("Selected subject " + optionId + " does not belong to any valid option group");
        }

        for (OptionGroupResponse group : curriculum.options()) {
            List<String> selected = selectionsByGroup.getOrDefault(group.groupId(), List.of());
            if (selected.size() < group.select())
                throw new RuleException(
                        "You must select at least " + group.select() + " subject(s) for " + group.name());
            if (selected.size() > group.list().size())
                throw new RuleException(
                        "You can only select up to " + group.list().size() + " subject(s) for " + group.name());
        }

        for (String optionId : selectedOptionIds) {
            var subject = resolveOptionalSubject(enrollment, optionId);
            var id = rg.generate("ENROLLMENT_SUBJECT", "ERM");
            var record = EnrollmentSubject.create(id, enrollment.getSchoolId(), enrollment,
                    subject, enrollment.getStudent());
            enrollmentSubjectRepo.save(record);
        }
    }

    private Subject resolveOptionalSubject(Enrollment enrollment, String optionId) {
        if (enrollment.getClazz().getLevel() == Level.SSS) {
            if (enrollment.getStream() == null) {
                throw new RuleException("Stream is required for SSS class");
            }
            var streamSubject = streamSubjectRepo.findByStreamIdAndSubjectIdAndSchoolId(
                    enrollment.getStream().getId(), optionId, enrollment.getSchoolId())
                    .orElseThrow(() -> new NotFoundException("Subject not found in stream"));
            return streamSubject.getSubject();
        }

        var classSubject = classSubjectRepo.findByClassIdAndSubjectId(
                enrollment.getClazz().getId(), optionId, enrollment.getSchoolId())
                .orElseThrow(() -> new NotFoundException("Subject not found in class"));
        return classSubject.getSubject();
    }

    private ClassSubjectResponse getSubjectCurriculum(Enrollment enrollment) {
        if (enrollment.getClazz().getLevel() == Level.SSS) {
            if (enrollment.getStream() == null) {
                throw new RuleException("Stream is required for SSS class");
            }
            return getStreamSubjects(enrollment.getSchoolId(), enrollment.getStream().getId());
        }
        return getClassSubjects(enrollment.getSchoolId(), enrollment.getClazz().getId());
    }

    private ClassSubjectResponse getClassSubjects(String schoolId, String classId) {
        var subjects = classSubjectRepo.findAllByClassIdAndSchoolId(classId, schoolId, Pageable.unpaged()).getContent();

        List<SubjectDTO> coreSubjects = subjects.stream()
                .filter(cs -> cs.getGroup() == null)
                .sorted(Comparator.comparing(cs -> cs.getSubject().getName()))
                .map(cs -> SubjectMapper.toDTO(cs.getSubject()))
                .toList();

        Map<String, List<ClassSubject>> grouped = subjects.stream()
                .filter(cs -> cs.getGroup() != null)
                .collect(Collectors.groupingBy(cs -> cs.getGroup().getId()));

        List<OptionGroupResponse> options = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<ClassSubject> groupSubjects = entry.getValue();
            SubjectGroup group = groupSubjects.get(0).getGroup();
            List<SubjectDTO> optionList = groupSubjects.stream()
                    .sorted(Comparator.comparing(cs -> cs.getSubject().getName()))
                    .map(cs -> SubjectMapper.toDTO(cs.getSubject()))
                    .toList();
            options.add(new OptionGroupResponse(group.getId(), group.getName(), group.getTotalSelection(), optionList));
        }

        return new ClassSubjectResponse(coreSubjects, options);
    }

    private ClassSubjectResponse getStreamSubjects(String schoolId, String streamId) {
        var subjects = streamSubjectRepo.findAllByStreamIdAndSchoolId(streamId, schoolId, Pageable.unpaged())
                .getContent();

        List<SubjectDTO> coreSubjects = subjects.stream()
                .filter(ss -> Boolean.TRUE.equals(ss.getMandatory()))
                .sorted(Comparator.comparing(ss -> ss.getSubject().getName()))
                .map(ss -> SubjectMapper.toDTO(ss.getSubject()))
                .toList();

        Map<String, List<StreamSubject>> grouped = subjects.stream()
                .filter(ss -> Boolean.FALSE.equals(ss.getMandatory()))
                .filter(ss -> ss.getGroup() != null)
                .collect(Collectors.groupingBy(ss -> ss.getGroup().getId()));

        List<OptionGroupResponse> options = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<StreamSubject> groupSubjects = entry.getValue();
            SubjectGroup group = groupSubjects.get(0).getGroup();
            List<SubjectDTO> optionList = groupSubjects.stream()
                    .sorted(Comparator.comparing(ss -> ss.getSubject().getName()))
                    .map(ss -> SubjectMapper.toDTO(ss.getSubject()))
                    .toList();
            options.add(new OptionGroupResponse(group.getId(), group.getName(), group.getTotalSelection(), optionList));
        }

        return new ClassSubjectResponse(coreSubjects, options);
    }

    private void sendWelcomeEmail(School school, Student student) {
        var subdomain = school.getDomain() + ".skultem.space";
        var link = "https://" + subdomain + "/login";
        var parent = student.getParent();
        var email = parent.getUser().getEmail();
        var clazz = student.getSession().getName();
        var schoolName = school.getName();
        var name = student.getName() + " (" + student.getAdmissionNumber() + ")";
        var password = parent.getUser().getHint();

        mailService.sendParentEmail(email, name, clazz, password, link, schoolName, subdomain);
    }

    private void sendLinkEmail(School school, Student student) {
        var subdomain = school.getDomain() + ".skultem.space";
        var link = "https://" + subdomain + "/login";
        var parent = student.getParent();
        var email = parent.getUser().getEmail();
        var clazz = student.getSession().getName();
        var schoolName = school.getName();
        var name = student.getName() + " (" + student.getAdmissionNumber() + ")";
        var parentName = parent.getUser().getName();

        mailService.sendLinkParentEmail(email, name, clazz, parentName, link, schoolName, subdomain);
    }

    public String uploadPhoto(MultipartFile file, String studentId, String schoolId) throws Exception {

        try {
            if (file == null || file.isEmpty()) {
                throw new RuleException("Student photo is required");
            }

            String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "");
            int extensionStart = originalFilename.lastIndexOf(".");
            if (extensionStart < 0 || extensionStart == originalFilename.length() - 1) {
                throw new RuleException("Student photo must have a valid file extension");
            }

            String extension = originalFilename.substring(extensionStart).toLowerCase(Locale.ROOT);

            String fileName = studentId + extension;

            String path = schoolId + "/" + fileName;

            return storageService.uploadStudentFile(file, path);
        } catch (Exception e) {
            throw new FileUploadException("Failed to upload student photo for studentId=" + studentId);
        }
    }
}
