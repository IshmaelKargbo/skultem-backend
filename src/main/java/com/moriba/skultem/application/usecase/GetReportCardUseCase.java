package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ReportCardMapper;
import com.moriba.skultem.application.services.SchoolBrandingResolver;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.infrastructure.security.PermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetReportCardUseCase {
    private final ReportCardRepository repo;
    private final SchoolRepository schoolRepo;
    private final ClassRepository classRepo;
    private final SchoolBrandingResolver brandingResolver;
    private final GetReportCardSettingUseCase getReportCardSettingUseCase;
    private final PermissionService permissionService;
    private final TeacherRepository teacherRepo;

    public ReportCardDTO execute(String schoolId, String id) {
        var card = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Report card not found"));

        // The card carries its class's management section's own logo / principal / location when it has them.
        var level = classRepo.findByIdAndSchool(card.getClassId(), schoolId).map(Clazz::getLevel).orElse(null);
        var school = schoolRepo.findById(schoolId)
                .map(s -> brandingResolver.schoolDtoFor(s, level))
                .orElse(null);

        ReportCardSettingDTO settings = getReportCardSettingUseCase.execute(schoolId);

        var dto = ReportCardMapper.toDTO(card, school, settings, level);

        // The class teacher's signature. Approval stamps it on the card; a card without one (still awaiting review,
        // approved before the teacher saved a signature, or approved by an admin) shows their current saved one.
        if (dto.classTeacherSignature() == null || dto.classTeacherSignature().isBlank()) {
            for (var userId : permissionService.classMasterUserIds(schoolId, card.getClassId())) {
                var teacher = teacherRepo.findByUserIdAndSchoolId(userId, schoolId).orElse(null);
                if (teacher == null || teacher.getSignature() == null || teacher.getSignature().isBlank()) {
                    continue;
                }
                String name = teacher.getUser() != null ? teacher.getUser().getName() : dto.classTeacherName();
                return withSigner(dto, dto.classTeacherName() != null ? dto.classTeacherName() : name, teacher.getSignature());
            }
        }
        return dto;
    }

    private static ReportCardDTO withSigner(ReportCardDTO d, String name, String signature) {
        return new ReportCardDTO(d.id(), d.studentId(), d.studentName(), d.admissionNumber(), d.photo(), d.classId(),
                d.className(), d.classSize(), d.termId(), d.termName(), d.academicYearName(), d.average(), d.position(),
                d.overallGrade(), d.passed(), d.attendancePercentage(), d.remark(), d.subjects(), d.generatedAt(),
                d.downloadCount(), d.school(), d.settings(), d.level(), d.scopeLabel(), d.gradeRemark(), d.reviewStatus(),
                d.reviewNote(), d.reviewedByName(), d.reviewedAt(), name, signature);
    }
}
