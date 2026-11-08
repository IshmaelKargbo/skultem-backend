package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A teacher's own signature, drawn or uploaded on their profile. It is printed on the report cards they approve as
// class teacher (copied onto each card when they approve it).
@Service
@Transactional
@RequiredArgsConstructor
public class TeacherSignatureUseCase {

    private static final long MAX_BYTES = 2 * 1024 * 1024;

    private final TeacherRepository teacherRepo;
    private final R2StorageService storageService;

    public record Result(String signature) {
    }

    public Result get(String schoolId, String userId) {
        return new Result(teacher(schoolId, userId).getSignature());
    }

    @AuditLogAnnotation(action = "TEACHER_SIGNATURE_SAVED")
    public Result save(String schoolId, String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuleException("Draw or choose your signature first");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new RuleException("The signature image is too large - keep it under 2 MB");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (!type.startsWith("image/")) {
            throw new RuleException("The signature has to be an image (PNG or JPG)");
        }
        var teacher = teacher(schoolId, userId);
        String path = "schools/" + schoolId + "/teacher-signatures/" + teacher.getId() + "-" + System.currentTimeMillis()
                + (type.contains("jpeg") || type.contains("jpg") ? ".jpg" : ".png");
        try {
            teacher.updateSignature(storageService.uploadBranding(file, path));
        } catch (Exception e) {
            throw new RuleException("Couldn't save your signature: " + e.getMessage());
        }
        teacherRepo.save(teacher);
        return new Result(teacher.getSignature());
    }

    @AuditLogAnnotation(action = "TEACHER_SIGNATURE_REMOVED")
    public Result remove(String schoolId, String userId) {
        var teacher = teacher(schoolId, userId);
        teacher.updateSignature(null);
        teacherRepo.save(teacher);
        return new Result(null);
    }

    private com.moriba.skultem.domain.model.Teacher teacher(String schoolId, String userId) {
        return teacherRepo.findByUserIdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new NotFoundException("No staff record for this account yet - ask the school office to add you to staff"));
    }
}
