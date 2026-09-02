package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.FileUploadException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Sets or replaces the photo on a User account - shared across every role that account holds
// (Teacher, Parent, or a plain account role), since it's one photo per person. See
// UpdateStudentPhotoUseCase for the equivalent on Student, a separate aggregate with its own photo.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateUserPhotoUseCase {
    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;
    private final R2StorageService storageService;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "USER_PHOTO_UPDATED")
    public UserDTO execute(String schoolId, String userId, MultipartFile photo) {
        var user = repo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        String photoUrl = uploadPhoto(photo, user.getId());
        user.setPhoto(photoUrl);
        repo.save(user);

        logActivityUseCase.log(schoolId, ActivityType.USER, "User photo updated", user.getName(), null,
                user.getId());

        List<Role> roles = schoolUserRepo.findAllByUser_IdAndSchoolId(user.getId(), schoolId).stream()
                .map(e -> e.getRole())
                .toList();

        return UserMapper.toDTO(user, roles);
    }

    private String uploadPhoto(MultipartFile file, String userId) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuleException("A photo is required");
            }

            String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "");
            int extensionStart = originalFilename.lastIndexOf(".");
            if (extensionStart < 0 || extensionStart == originalFilename.length() - 1) {
                throw new RuleException("Photo must have a valid file extension");
            }

            String extension = originalFilename.substring(extensionStart).toLowerCase(Locale.ROOT);
            // Deliberately not scoped under schoolId like student/school-asset uploads - a User's
            // photo follows the person across every school they belong to, not just the active one.
            String path = "users/" + userId + extension;

            return storageService.uploadFile(file, path);
        } catch (RuleException e) {
            throw e;
        } catch (Exception e) {
            throw new FileUploadException("Failed to upload photo for userId=" + userId);
        }
    }
}
