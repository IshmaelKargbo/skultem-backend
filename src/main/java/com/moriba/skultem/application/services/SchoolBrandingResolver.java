package com.moriba.skultem.application.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

// Works out which logo / principal / signature / address to print for a level. A SECTION_BASED
// school can give each management section its own (a school run from different places, with a
// different head for each); anything the section leaves blank falls back to the school's own, and
// a UNIFIED school - or a level with no section - simply gets the school's.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolBrandingResolver {

    // ownPrincipal / ownAddress say whether the principal / address came from a section rather
    // than the school - a document with its own school-wide design override (the ID card's
    // "school address" and "principal") should let a section's own value win over that override.
    public record EffectiveBranding(String logo, String principalName, String principalSignature, Address address,
            boolean ownPrincipal, boolean ownAddress) {
    }

    private final SchoolLevelRepository schoolLevelRepo;
    private final ManagementSectionRepository managementSectionRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final TeacherRepository teacherRepo;
    private final PaymentRepository paymentRepo;
    private final StaffManagementSectionRepository staffSectionRepo;

    // The school as a DTO, with its logo / principal / signature / address swapped for the level's
    // section's own where it has them - for documents (report cards) that print the school block.
    public SchoolDTO schoolDtoFor(School school, Level level) {
        var dto = SchoolMapper.toDTO(school);
        var b = forLevel(school, level);
        return new SchoolDTO(dto.id(), dto.name(), dto.domain(), b.address(), dto.owner(), dto.status(),
                dto.gradingScale(), b.logo(), dto.motto(), b.principalName(), b.principalSignature(),
                dto.primaryColor(), dto.secondaryColor(), dto.attendanceThreshold(), dto.genderComposition(),
                dto.testSchool(), dto.managementModel(), dto.createdAt(), dto.updatedAt());
    }

    public EffectiveBranding forSchool(School school) {
        return new EffectiveBranding(school.getLogo(), school.getPrincipalName(), school.getPrincipalSignature(),
                school.getAddress(), false, false);
    }

    // A student prints their current class's section (their most recent enrollment).
    public EffectiveBranding forStudent(School school, String studentId) {
        var level = studentId == null ? null
                : enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(studentId, school.getId())
                        .map(e -> e.getClazz().getLevel()).orElse(null);
        return forLevel(school, level);
    }

    // A receipt covers one student's payments (one reference number) - that student's section.
    public EffectiveBranding forReceipt(School school, String referenceNo) {
        var studentId = referenceNo == null ? null
                : paymentRepo.findAllByReferenceNoAndSchoolId(referenceNo, school.getId()).stream()
                        .filter(p -> p.getStudent() != null)
                        .map(p -> p.getStudent().getId())
                        .findFirst().orElse(null);
        return forStudent(school, studentId);
    }

    // A staff member prints their section when they're limited to exactly one; whole-school staff,
    // or staff spread over several sections, print the school's own (no single section is "theirs").
    public EffectiveBranding forTeacher(School school, String teacherId) {
        if (teacherId == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return forSchool(school);
        }
        var sectionIds = teacherRepo.findByIdAndSchoolId(teacherId, school.getId())
                .map(t -> staffSectionRepo.findBySchoolAndUserAndRole(school.getId(), t.getUser().getId(), Role.TEACHER)
                        .stream().map(a -> a.getManagementSectionId()).distinct().toList())
                .orElse(List.<String>of());
        return sectionIds.size() == 1 ? forSection(school, sectionIds.get(0)) : forSchool(school);
    }

    public EffectiveBranding forLevel(School school, Level level) {
        if (level == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return forSchool(school);
        }
        String sectionId = schoolLevelRepo.findBySchoolId(school.getId()).stream()
                .filter(l -> l.getLevel() == level)
                .map(SchoolLevel::getManagementSectionId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
        return forSection(school, sectionId);
    }

    public EffectiveBranding forSection(School school, String sectionId) {
        if (sectionId == null || school.getManagementModel() != ManagementModel.SECTION_BASED) {
            return forSchool(school);
        }
        return managementSectionRepo.findBySchoolId(school.getId()).stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .map(s -> merge(school, s))
                .orElseGet(() -> forSchool(school));
    }

    private static EffectiveBranding merge(School school, ManagementSection section) {
        return new EffectiveBranding(
                firstNonBlank(section.getLogo(), school.getLogo()),
                firstNonBlank(section.getPrincipalName(), school.getPrincipalName()),
                firstNonBlank(section.getPrincipalSignature(), school.getPrincipalSignature()),
                hasAddress(section.getAddress()) ? section.getAddress() : school.getAddress(),
                notBlank(section.getPrincipalName()), hasAddress(section.getAddress()));
    }

    public static boolean hasAddress(Address a) {
        return a != null && (notBlank(a.region()) || notBlank(a.district()) || notBlank(a.chiefdom())
                || notBlank(a.city()) || notBlank(a.street()));
    }

    private static String firstNonBlank(String a, String b) {
        return notBlank(a) ? a : b;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
