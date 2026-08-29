package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReceiptSettingDTO;
import com.moriba.skultem.application.mapper.ReceiptSettingMapper;
import com.moriba.skultem.domain.repository.ReceiptSettingRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetReceiptSettingUseCase {
    private final ReceiptSettingRepository repo;
    private final SchoolRepository schoolRepo;

    // Deliberately the raw R2 URL, not a resolved data: URI - this value also seeds the editable
    // "Logo URL" field on the settings page and gets saved back into a bounded logo_url column,
    // and a base64 data: URI would be way too large for either. GetSchoolBrandingAssetsUseCase's
    // data: URI conversion (needed so a cross-origin image survives html2canvas's PDF capture) is
    // applied at capture time instead - see fee/payment/new.vue and receipt/viewer.vue.
    public ReceiptSettingDTO execute(String schoolId) {
        var existing = repo.findBySchoolId(schoolId);
        if (existing.isPresent()) {
            return ReceiptSettingMapper.toDTO(existing.get());
        }

        var school = schoolRepo.findById(schoolId).orElse(null);
        String accentColor = school != null && school.getPrimaryColor() != null ? school.getPrimaryColor()
                : "#4338ca";
        String logoUrl = school != null && school.getLogo() != null ? school.getLogo() : "";

        return new ReceiptSettingDTO(accentColor, logoUrl, "", true, true);
    }
}
