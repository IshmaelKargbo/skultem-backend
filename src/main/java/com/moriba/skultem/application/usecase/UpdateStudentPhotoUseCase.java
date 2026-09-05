package com.moriba.skultem.application.usecase;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.FileUploadException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A student can be enrolled without a photo (see CreateStudentUseCase) - this is how it gets added
// or replaced afterwards, from their profile page.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateStudentPhotoUseCase {
    // See R2StorageService.uploadPhoto - keeps a full-resolution phone photo from being served
    // unchanged to every student list's avatar.
    private static final int MAX_PHOTO_DIMENSION = 512;

    private final StudentRepository repo;
    private final StudentParentRepository studentParentRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final R2StorageService storageService;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_PHOTO_UPDATED")
    public StudentDTO execute(String schoolId, String studentId, MultipartFile photo) {
        var student = repo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        String photoUrl = uploadPhoto(photo, student.getId(), schoolId);
        student.setProfile(photoUrl);
        repo.save(student);

        logActivityUseCase.log(schoolId, ActivityType.STUDENT, "Student photo updated",
                student.getGivenNames() + " " + student.getFamilyName() + " - " + student.getAdmissionNumber(), null,
                student.getId());

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, null);
        var enrollment = enrollmentRepo
                .findByStudentAndAcademicYearAndSchoolId(student.getId(), academicYear.getId(), schoolId)
                .or(() -> enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(student.getId(), schoolId))
                .orElse(null);
        var relationship = studentParentRepo.findByStudentAndSchool(student.getId(), schoolId)
                .map(sp -> sp.getRelationship())
                .orElse(null);

        return StudentMapper.toDTO(student, enrollment, relationship);
    }

    private String uploadPhoto(MultipartFile file, String studentId, String schoolId) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuleException("A photo is required");
            }

            String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "");
            int extensionStart = originalFilename.lastIndexOf(".");
            if (extensionStart < 0 || extensionStart == originalFilename.length() - 1) {
                throw new RuleException("Photo must have a valid file extension");
            }

            // The extension itself no longer feeds into the storage path - uploadPhoto always
            // re-encodes as JPEG - but the check above still catches a file with no extension at
            // all as a basic sanity check.
            String basePath = schoolId + "/" + studentId;

            return storageService.uploadPhoto(file, basePath, MAX_PHOTO_DIMENSION);
        } catch (RuleException e) {
            throw e;
        } catch (Exception e) {
            throw new FileUploadException("Failed to upload student photo for studentId=" + studentId);
        }
    }
}
