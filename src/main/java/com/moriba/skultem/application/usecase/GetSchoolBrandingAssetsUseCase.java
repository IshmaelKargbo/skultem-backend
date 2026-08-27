package com.moriba.skultem.application.usecase;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolBrandingAssetsDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Fetches the school's logo/principal signature as inline data: URIs rather
// than handing back the raw R2 URL - see the comment on
// R2StorageService.downloadAsDataUri for why (R2's public bucket sends no
// CORS headers, which breaks html2canvas's PDF capture of the ID card).
// The two downloads run concurrently (each is its own network round trip to
// R2) rather than one after the other.
@Service
@Transactional
@RequiredArgsConstructor
public class GetSchoolBrandingAssetsUseCase {

    private final SchoolRepository repo;
    private final R2StorageService storageService;

    public SchoolBrandingAssetsDTO execute(String schoolId) {
        School school = repo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));

        CompletableFuture<String> logo = CompletableFuture.supplyAsync(
                () -> storageService.downloadAsDataUri(school.getLogo()));
        CompletableFuture<String> signature = CompletableFuture.supplyAsync(
                () -> storageService.downloadAsDataUri(school.getPrincipalSignature()));

        return new SchoolBrandingAssetsDTO(logo.join(), signature.join());
    }
}
