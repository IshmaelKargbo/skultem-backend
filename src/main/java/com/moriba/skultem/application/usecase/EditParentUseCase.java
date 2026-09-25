package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ParentDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.ParentMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A guardian's own details: name, phone and address, plus - when they already have one - the email
// they log in with. A parent with no email gets one through AddParentEmailUseCase instead, since that
// also sends them their login details.
@Service
@Transactional
@RequiredArgsConstructor
public class EditParentUseCase {

    private final ParentRepository parentRepo;
    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PARENT_EDITED")
    public ParentDTO execute(String schoolId, String parentId, String givenNames, String familyName, String phone,
            String street, String city, String email) {
        var parent = parentRepo.findByIdAndSchoolId(parentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Parent not found"));

        String normalizedPhone = phone.trim();
        if (parentRepo.existsByPhoneAndSchoolAndIdNot(normalizedPhone, schoolId, parentId)) {
            throw new AlreadyExistsException("Another parent in this school already uses that phone number");
        }

        var user = parent.getUser();
        String newEmail = email == null || email.isBlank() ? null : email.trim();
        boolean emailChanging = newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail());

        if (emailChanging) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                throw new RuleException("Use \"Add Email\" for a parent without an email - it also sends them "
                        + "their login details.");
            }
            // The email is the login - only safe to change when this parent account is used for nothing else.
            boolean shared = schoolUserRepo.findAllByUser_Id(user.getId()).stream()
                    .anyMatch(m -> m.getRole() != Role.PARENT || !schoolId.equals(m.getSchoolId()));
            if (shared) {
                throw new RuleException("This person's login is also used for another role or school, so their "
                        + "email can't be changed here.");
            }
            if (userRepo.existsByEmail(newEmail)) {
                throw new AlreadyExistsException("Email already in use by another account");
            }
            user.changeEmail(newEmail);
        }

        user.update(givenNames.trim(), familyName.trim());
        userRepo.save(user);

        parent.updateContact(normalizedPhone, street.trim(), city.trim());
        parentRepo.save(parent);

        logActivityUseCase.log(schoolId, ActivityType.PARENT, "Parent updated",
                user.getGivenNames() + " " + user.getFamilyName(), emailChanging ? "Email changed" : null,
                parent.getId());

        return ParentMapper.toDTO(parent);
    }
}
