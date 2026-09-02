package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserPayrollStatusDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Title;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Backfills a payroll/staff record (a Teacher row) onto a User account created before payroll
// inclusion existed, or created without checking the box - from their profile page. Mirrors
// CreateUserUseCase.addToPayroll, just for a user that already exists rather than one just
// created. Never grants Role.TEACHER - their account keeps whatever role(s) they already have.
@Service
@Transactional
@RequiredArgsConstructor
public class IncludeUserInPayrollUseCase {
    private final UserRepository userRepo;
    private final TeacherRepository teacherRepo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "USER_INCLUDED_IN_PAYROLL")
    public UserPayrollStatusDTO execute(String schoolId, String userId, String staffId, String phone, String street,
            String city, String gender, String title, String designation) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        if (teacherRepo.findByUserId(userId).isPresent()) {
            throw new AlreadyExistsException("This user is already on payroll");
        }

        if (isBlank(staffId) || isBlank(phone) || isBlank(street) || isBlank(city) || isBlank(gender)
                || isBlank(title)) {
            throw new BadRequestException(
                    "Staff ID, phone, street, city, gender and title are required to include this user in payroll");
        }

        if (teacherRepo.existsByStaffIdAndSchool(schoolId, staffId)) {
            throw new AlreadyExistsException("staffId already exist in this school");
        }

        if (teacherRepo.existsByPhoneAndSchool(schoolId, phone)) {
            throw new AlreadyExistsException("phone already exist in this school");
        }

        var teacherId = rg.generate("TEACHER", "THR");
        var teacher = Teacher.create(teacherId, schoolId, Title.valueOf(title), phone, street, city,
                Gender.valueOf(gender), staffId, user, designation, false);
        teacherRepo.save(teacher);

        logActivityUseCase.log(
                schoolId,
                ActivityType.USER,
                "User included in payroll",
                user.getGivenNames() + " " + user.getFamilyName() + " - " + staffId,
                null,
                user.getId());

        return new UserPayrollStatusDTO(true, teacher.getId(), teacher.getStaffId(), teacher.getDesignation(),
                teacher.isTeaching());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
