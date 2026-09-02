package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.PayslipSettingDTO;
import com.moriba.skultem.application.mapper.PayslipSettingMapper;
import com.moriba.skultem.domain.repository.PayslipSettingRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPayslipSettingUseCase {
    private final PayslipSettingRepository repo;
    private final SchoolRepository schoolRepo;

    // Same reasoning as GetReceiptSettingUseCase: the raw R2 logo URL, not a resolved data: URI -
    // this value also seeds the editable "Logo URL" field on the settings page. The data: URI
    // conversion needed to survive html2canvas's PDF capture happens client-side at capture time.
    public PayslipSettingDTO execute(String schoolId) {
        var existing = repo.findBySchoolId(schoolId);
        if (existing.isPresent()) {
            return PayslipSettingMapper.toDTO(existing.get());
        }

        var school = schoolRepo.findById(schoolId).orElse(null);
        String accentColor = school != null && school.getPrimaryColor() != null ? school.getPrimaryColor()
                : "#4338ca";
        String logoUrl = school != null && school.getLogo() != null ? school.getLogo() : "";

        return new PayslipSettingDTO(accentColor, logoUrl, "", true, true);
    }
}
