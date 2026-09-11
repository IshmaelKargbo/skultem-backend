package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.IdCardSettingDTO;
import com.moriba.skultem.application.mapper.IdCardSettingMapper;
import com.moriba.skultem.domain.repository.IdCardSettingRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetIdCardSettingUseCase {
    private static final String DEFAULT_FIELDS = "["
            + "{\"key\":\"name\",\"label\":\"Full Name\",\"icon\":\"i-lucide-user\",\"cardSlot\":\"front\",\"enabled\":true,\"required\":true},"
            + "{\"key\":\"admissionNo\",\"label\":\"Admission No.\",\"icon\":\"i-lucide-id-card\",\"cardSlot\":\"front\",\"enabled\":true,\"required\":true},"
            + "{\"key\":\"class\",\"label\":\"Class\",\"icon\":\"i-lucide-school\",\"cardSlot\":\"front\",\"enabled\":true},"
            + "{\"key\":\"gender\",\"label\":\"Gender\",\"icon\":\"i-lucide-user-round\",\"cardSlot\":\"front\",\"enabled\":true},"
            + "{\"key\":\"dob\",\"label\":\"Date of Birth\",\"icon\":\"i-lucide-calendar-days\",\"cardSlot\":\"front\",\"enabled\":true},"
            + "{\"key\":\"expiryDate\",\"label\":\"Valid Until\",\"icon\":\"i-lucide-calendar-check\",\"cardSlot\":\"front\",\"enabled\":true},"
            + "{\"key\":\"parentContact\",\"label\":\"Parent Contact\",\"icon\":\"i-lucide-phone\",\"cardSlot\":\"back\",\"enabled\":true},"
            + "{\"key\":\"emergencyContact\",\"label\":\"Emergency Contact\",\"icon\":\"i-lucide-phone-call\",\"cardSlot\":\"back\",\"enabled\":true}"
            + "]";

    // Staff cards reuse the same colour/layout/dimensions settings as students - only the field
    // list differs (Staff ID/Position/Phone instead of Admission No/Class/Guardian).
    private static final String DEFAULT_STAFF_FIELDS = "["
            + "{\"key\":\"name\",\"label\":\"Full Name\",\"icon\":\"i-lucide-user\",\"cardSlot\":\"front\",\"enabled\":true,\"required\":true},"
            + "{\"key\":\"staffId\",\"label\":\"Staff ID\",\"icon\":\"i-lucide-id-card\",\"cardSlot\":\"front\",\"enabled\":true,\"required\":true},"
            + "{\"key\":\"designation\",\"label\":\"Position\",\"icon\":\"i-lucide-briefcase\",\"cardSlot\":\"front\",\"enabled\":true},"
            + "{\"key\":\"gender\",\"label\":\"Gender\",\"icon\":\"i-lucide-user-round\",\"cardSlot\":\"front\",\"enabled\":true},"
            + "{\"key\":\"expiryDate\",\"label\":\"Valid Until\",\"icon\":\"i-lucide-calendar-check\",\"cardSlot\":\"front\",\"enabled\":true},"
            + "{\"key\":\"phone\",\"label\":\"Phone\",\"icon\":\"i-lucide-phone\",\"cardSlot\":\"back\",\"enabled\":true}"
            + "]";

    private final IdCardSettingRepository repo;
    private final SchoolRepository schoolRepo;

    public IdCardSettingDTO execute(String schoolId) {
        var existing = repo.findBySchoolId(schoolId);
        if (existing.isPresent()) {
            var dto = IdCardSettingMapper.toDTO(existing.get());
            // Rows saved before staff cards existed have a null staffFields column - backfill it
            // on read rather than requiring a SQL migration backfill or a resave.
            if (dto.staffFields() == null || dto.staffFields().isBlank()) {
                dto = new IdCardSettingDTO(dto.layout(), dto.profileShape(), dto.headerColor(), dto.footerColor(),
                        dto.headerTextColor(), dto.primaryTextColor(), dto.widthMm(), dto.heightMm(),
                        dto.bgImageUrl(), dto.bgOpacity(), dto.schoolName(), dto.schoolAddress(),
                        dto.principalName(), dto.fields(), DEFAULT_STAFF_FIELDS, dto.validityYears());
            }
            return dto;
        }

        var school = schoolRepo.findById(schoolId).orElse(null);
        String schoolName = school != null ? school.getName() : "";
        String schoolAddress = "";
        if (school != null && school.getAddress() != null) {
            var address = school.getAddress();
            schoolAddress = java.util.stream.Stream
                    .of(address.street(), address.city(), address.chiefdom(), address.district(), address.region())
                    .filter(part -> part != null && !part.isBlank())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }

        return new IdCardSettingDTO("vertical", "square", "#1878c5", "#1878c5", "#ffffff", "#111827", 85, 54, "", 20,
                schoolName, schoolAddress, "", DEFAULT_FIELDS, DEFAULT_STAFF_FIELDS, 1);
    }
}
