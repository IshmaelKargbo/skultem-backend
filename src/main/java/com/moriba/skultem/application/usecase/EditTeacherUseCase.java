package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Title;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class EditTeacherUseCase {

    private final TeacherRepository repo;
    private final UserRepository userRepo;
    private final SchoolRepository schoolRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_EDITED")
    public TeacherDTO execute(String schoolId, String teacherId, Title title, String givenNames, String familyName,
            Gender gender, String staffId, String phone, String street, String city) {

        schoolRepo.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("School not found"));

        Teacher teacher = repo.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        User user = teacher.getUser();

        String oldTitle = teacher.getTitle() != null ? teacher.getTitle().name() : null;
        String oldPhone = teacher.getPhone();
        String oldStreet = teacher.getStreet();
        String oldCity = teacher.getCity();
        String oldStaffId = teacher.getStaffId();
        String oldGender = teacher.getGender() != null ? teacher.getGender().name() : null;
        String oldName = user.getGivenNames() + " " + user.getFamilyName();

        if (repo.existsByStaffIdAndSchoolAndIdNot(schoolId, staffId, teacherId)) {
            throw new AlreadyExistsException("Staff ID already exists in this school");
        }

        if (repo.existsByPhoneAndSchoolAndIdNot(schoolId, phone, teacherId)) {
            throw new AlreadyExistsException("Phone already exists in this school");
        }

        user = teacher.getUser();
        user.update(givenNames, familyName);
        userRepo.save(user);

        teacher.update(title, phone, street, city, gender, staffId);
        repo.save(teacher);

        String meta = buildMeta(
                oldName,
                user.getGivenNames() + " " + user.getFamilyName(),
                oldTitle,
                title != null ? title.name() : null,
                oldPhone,
                phone,
                oldStreet,
                street,
                oldCity,
                city,
                oldGender,
                gender != null ? gender.name() : null,
                oldStaffId,
                staffId);

        logActivityUseCase.log(
                schoolId,
                ActivityType.TEACHER,
                "Teacher updated",
                user.getGivenNames() + " " + user.getFamilyName() + " - " + teacher.getStaffId(),
                meta,
                teacher.getId());

        return TeacherMapper.toDTO(teacher);
    }

    private String buildMeta(String oldName, String newName, String oldTitle, String newTitle, String oldPhone,
            String newPhone, String oldStreet, String newStreet, String oldCity, String newCity, String oldGender,
            String newGender, String oldStaffId, String newStaffId) {
        StringBuilder meta = new StringBuilder();

        append(meta, "name", oldName, newName);
        append(meta, "title", oldTitle, newTitle);
        append(meta, "phone", oldPhone, newPhone);
        append(meta, "street", oldStreet, newStreet);
        append(meta, "city", oldCity, newCity);
        append(meta, "gender", oldGender, newGender);
        append(meta, "staffId", oldStaffId, newStaffId);

        return meta.toString();
    }

    private void append(StringBuilder meta, String field, String oldVal, String newVal) {
        if (oldVal == null && newVal == null)
            return;
        if (oldVal != null && oldVal.equals(newVal))
            return;

        if (meta.length() > 0)
            meta.append(", ");

        meta.append(field)
                .append(": ")
                .append(oldVal)
                .append(" → ")
                .append(newVal);
    }
}