package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssetDataUriDTO;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import lombok.RequiredArgsConstructor;

// Same idea as GetSchoolBrandingAssetsUseCase (R2's public bucket sends no CORS headers, which
// breaks html2canvas's PDF/print capture of a cross-origin image), generalised to any single R2
// URL - a student or staff photo on an ID card, say, rather than only the school's logo/signature.
// downloadAsDataUri() already refuses anything outside this app's own bucket, so there's no
// open-fetch/SSRF risk in accepting an arbitrary url here.
@Service
@RequiredArgsConstructor
public class GetAssetDataUriUseCase {

    private final R2StorageService storageService;

    public AssetDataUriDTO execute(String url) {
        return new AssetDataUriDTO(storageService.downloadAsDataUri(url));
    }
}
