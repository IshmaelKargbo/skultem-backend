package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.application.dto.SchoolStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.SchoolBrandingResolver;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;

import lombok.RequiredArgsConstructor;

// Sets one management section's own logo / principal / signature / address. A blank field means
// "use the school's" - that's how a section goes back to inheriting. Files are only replaced when
// a new one is sent; removeLogo / removeSignature clear them explicitly.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSectionBrandingUseCase {

    public record Input(String principalName, Address address, MultipartFile logo, MultipartFile principalSignature,
            boolean removeLogo, boolean removeSignature) {
    }

    private final SchoolRepository schoolRepo;
    private final ManagementSectionRepository sectionRepo;
    private final R2StorageService storageService;
    private final GetSchoolStructureUseCase getSchoolStructureUseCase;

    public SchoolStructureDTO execute(String schoolId, String sectionId, Input in) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        if (school.getManagementModel() != ManagementModel.SECTION_BASED) {
            throw new RuleException("This school isn't managed in sections");
        }
        var section = sectionRepo.findBySchoolId(schoolId).stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Management section not found"));

        String logo = section.getLogo();
        if (in.logo() != null && !in.logo().isEmpty()) {
            logo = upload(in.logo(), schoolId, sectionId, "logo");
        } else if (in.removeLogo()) {
            logo = null;
        }

        String signature = section.getPrincipalSignature();
        if (in.principalSignature() != null && !in.principalSignature().isEmpty()) {
            signature = upload(in.principalSignature(), schoolId, sectionId, "principal-signature");
        } else if (in.removeSignature()) {
            signature = null;
        }

        Address address = clean(in.address());
        section.updateBranding(logo, blankToNull(in.principalName()),
                signature, SchoolBrandingResolver.hasAddress(address) ? address : null);
        sectionRepo.save(section);

        return getSchoolStructureUseCase.execute(schoolId);
    }

    private Address clean(Address a) {
        if (a == null) {
            return null;
        }
        return new Address(blankToNull(a.region()), blankToNull(a.district()), blankToNull(a.chiefdom()),
                blankToNull(a.city()), blankToNull(a.street()));
    }

    private String upload(MultipartFile file, String schoolId, String sectionId, String label) {
        String name = file.getOriginalFilename();
        String extension = name == null || !name.contains(".") ? "" : name.substring(name.lastIndexOf('.'));
        String path = "schools/" + schoolId + "/sections/" + sectionId + "/" + label + "-"
                + System.currentTimeMillis() + extension;
        try {
            return storageService.uploadBranding(file, path);
        } catch (Exception e) {
            throw new RuleException("Failed to upload " + label.replace('-', ' ') + ": " + e.getMessage());
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
