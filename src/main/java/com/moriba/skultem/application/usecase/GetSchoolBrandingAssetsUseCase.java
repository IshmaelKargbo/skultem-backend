package com.moriba.skultem.application.usecase;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolBrandingAssetsDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.SchoolBrandingResolver;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Fetches the logo/principal signature as inline data: URIs rather than handing back the raw R2
// URL - see the comment on R2StorageService.downloadAsDataUri for why (R2's public bucket sends no
// CORS headers, which breaks html2canvas's PDF capture of the ID card and reports).
// For a level, student (their current class) or staff member the branding is their management
// section's (falling back to the school's); with no target it's the school's own. The two downloads run concurrently (each is its own network
// round trip to R2) rather than one after the other.
@Service
@Transactional
@RequiredArgsConstructor
public class GetSchoolBrandingAssetsUseCase {

    private final SchoolRepository repo;
    private final R2StorageService storageService;
    private final SchoolBrandingResolver brandingResolver;

    // What the branding is for. Exactly one is normally set; with none it's the school's own.
    public record Target(Level level, String studentId, String teacherId, String referenceNo) {
        public static Target school() {
            return new Target(null, null, null, null);
        }
    }

    public SchoolBrandingAssetsDTO execute(String schoolId) {
        return execute(schoolId, Target.school(), true);
    }

    public SchoolBrandingAssetsDTO execute(String schoolId, Target target, boolean inline) {
        var school = repo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));
        var branding = target.studentId() != null ? brandingResolver.forStudent(school, target.studentId())
                : target.referenceNo() != null ? brandingResolver.forReceipt(school, target.referenceNo())
                : target.teacherId() != null ? brandingResolver.forTeacher(school, target.teacherId())
                : brandingResolver.forLevel(school, target.level());

        if (!inline) {
            return new SchoolBrandingAssetsDTO(branding.logo(), branding.principalSignature(),
                    branding.principalName(), branding.address(), branding.ownPrincipal(), branding.ownAddress());
        }

        CompletableFuture<String> logo = CompletableFuture.supplyAsync(
                () -> storageService.downloadAsDataUri(branding.logo()));
        CompletableFuture<String> signature = CompletableFuture.supplyAsync(
                () -> storageService.downloadAsDataUri(branding.principalSignature()));

        return new SchoolBrandingAssetsDTO(logo.join(), signature.join(), branding.principalName(),
                branding.address(), branding.ownPrincipal(), branding.ownAddress());
    }
}
