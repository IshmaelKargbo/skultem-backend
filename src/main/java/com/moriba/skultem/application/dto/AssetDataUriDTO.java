package com.moriba.skultem.application.dto;

// See GetAssetDataUriUseCase - a generic counterpart to SchoolBrandingAssetsDTO for any single R2
// asset URL (a student/staff photo, say) rather than just the school's logo/signature pair.
public record AssetDataUriDTO(String dataUri) {
}
