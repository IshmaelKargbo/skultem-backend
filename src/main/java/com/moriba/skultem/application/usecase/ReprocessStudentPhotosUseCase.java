package com.moriba.skultem.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// One-off maintenance operation, not part of the normal enrollment/photo-update flow (see
// CreateStudentUseCase and UpdateStudentPhotoUseCase for that) - it walks every student in a
// school that already has a photo and re-hosts it through the same downscale pipeline a fresh
// upload goes through. Exists because students enrolled before that pipeline shipped (or via a
// direct data import) can have a `photo` pointing at a full-resolution file, or a file on a
// storage provider that's since gone away - both serve every UAvatar in the student list a much
// bigger download than it needs, or a broken image. A dead link gets cleared to null (so the UI
// falls back to the initials avatar) rather than left pointing at a request that will keep
// failing on every page load.
@Service
@Transactional
@RequiredArgsConstructor
public class ReprocessStudentPhotosUseCase {
    private static final Logger log = LoggerFactory.getLogger(ReprocessStudentPhotosUseCase.class);
    private static final int MAX_PHOTO_DIMENSION = 512;

    private final StudentRepository repo;
    private final R2StorageService storageService;

    public record Summary(int total, int reprocessed, int cleared) {
    }

    public Summary execute(String schoolId) {
        var students = repo.findBySchoolId(schoolId, Pageable.unpaged()).getContent();

        int reprocessed = 0;
        int cleared = 0;

        for (var student : students) {
            String photo = student.getPhoto();
            if (photo == null || photo.isBlank()) {
                continue;
            }

            String basePath = schoolId + "/" + student.getId();
            try {
                String newUrl = storageService.uploadPhotoFromUrl(photo, basePath, MAX_PHOTO_DIMENSION);
                student.setProfile(newUrl);
                repo.save(student);
                reprocessed++;
            } catch (Exception e) {
                log.warn("Could not reprocess photo for studentId={} (url={}), clearing it: {}", student.getId(),
                        photo, e.getMessage());
                student.setProfile(null);
                repo.save(student);
                cleared++;
            }
        }

        return new Summary(students.size(), reprocessed, cleared);
    }
}
