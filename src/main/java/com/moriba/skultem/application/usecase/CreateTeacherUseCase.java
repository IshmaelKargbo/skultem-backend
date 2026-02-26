package com.moriba.skultem.application.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateTeacherUseCase {

    private final UserRepository userRepo;
    private final SchoolUserRepository schoolUserRepo;
    private final TeacherRepository repo;
    private final ReferenceGeneratorUsecase rg;
    private final PasswordEncoder passwordEncoder;

    public TeacherDTO execute(String schoolId, String givenNames, String familyName, String staffId, String email,
            String phone, Address address) {
        var password = "teacher@123";

        User user;
        if (userRepo.existsByEmail(email)) {
            user = userRepo.findByEmail(email).orElseThrow();
        } else {
            var id = rg.generate("USER", "USR");
            var passwordHash = passwordEncoder.encode(password);
            user = User.create(id, givenNames, familyName, email, passwordHash);
            userRepo.save(user);
        }

        if (repo.existsByStaffIdAndSchool(schoolId, staffId)) {
            throw new AlreadyExistsException("staffId already exist in this school");
        }

        if (repo.existsByPhoneAndSchool(schoolId, staffId)) {
            throw new AlreadyExistsException("staffId already exist in this school");
        }

        if (schoolUserRepo.existsBySchoolAndUser(schoolId, user.getId())) {
            throw new AlreadyExistsException("user already exist in this school");
        }

        var schoolUserId = rg.generate("SCHOOL_USER", "SCU");
        var schoolUser = SchoolUser.create(schoolUserId, schoolId, user, Role.TEACHER);
        schoolUserRepo.save(schoolUser);

        var teacherId = rg.generate("TEACHER", "THR");
        var teacher = Teacher.create(teacherId, schoolId, phone, address, staffId, user);
        repo.save(teacher);

        return TeacherMapper.toDTO(teacher);
    }
}
