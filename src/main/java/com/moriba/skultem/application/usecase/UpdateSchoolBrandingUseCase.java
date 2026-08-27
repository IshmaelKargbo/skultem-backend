package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// School branding assets (logo, principal signature) live in their own
// Cloudflare R2 bucket, separate from student photos which stay on Supabase.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSchoolBrandingUseCase {

    private final SchoolRepository repo;
    private final R2StorageService storageService;

    public SchoolDTO execute(String schoolId, String principalName, MultipartFile logo,
            MultipartFile principalSignature, String primaryColor, String secondaryColor) {
        School school = repo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));

        String logoUrl = school.getLogo();
        if (logo != null && !logo.isEmpty()) {
            logoUrl = upload(logo, schoolId, "logo");
        }

        String signatureUrl = school.getPrincipalSignature();
        if (principalSignature != null && !principalSignature.isEmpty()) {
            signatureUrl = upload(principalSignature, schoolId, "principal-signature");
        }

        String name = principalName != null ? principalName : school.getPrincipalName();
        String primary = primaryColor != null && !primaryColor.isBlank() ? primaryColor : school.getPrimaryColor();
        String secondary = secondaryColor != null && !secondaryColor.isBlank() ? secondaryColor
                : school.getSecondaryColor();

        school.updateBranding(logoUrl, name, signatureUrl, primary, secondary);
        repo.save(school);
        return SchoolMapper.toDTO(school);
    }

    private String upload(MultipartFile file, String schoolId, String label) {
        String extension = resolveExtension(file.getOriginalFilename());
        String path = "schools/" + schoolId + "/" + label + "-" + System.currentTimeMillis() + extension;
        try {
            return storageService.uploadFile(file, path);
        } catch (Exception e) {
            throw new RuleException("Failed to upload " + label.replace('-', ' ') + ": " + e.getMessage());
        }
    }

    private String resolveExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
