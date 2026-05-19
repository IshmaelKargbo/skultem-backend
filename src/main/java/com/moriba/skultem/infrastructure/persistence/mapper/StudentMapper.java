package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.House;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.vo.Family;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

public class StudentMapper {
    public static Student toDomain(StudentEntity param) {
        if (param == null) {
            return null;
        }

        Family family = JsonMapper.fromJson(param.getFamily(), Family.class);
        Parent parent = ParentMapper.toDomain(param.getParent());
        ClassSession session = ClassSessionMapper.toDomain(param.getSession());
        House house = HouseMapper.toDomain(param.getHouse());

        return new Student(param.getId(), param.getSchoolId(), param.getPhoto(), param.getAdmissionNumber(),
                param.getAdmissionDate(), param.getGivenNames(), param.getFamilyName(), family, session,
                param.getLastClass(), param.getGender(), parent, param.getDateOfBirth(), param.getEnrollmentType(),
                param.getPreviousSchool(), house, param.getNationality(), param.getReligion(), param.getCity(),
                param.getStreet(), param.getStatus(), param.getCreatedAt(), param.getUpdatedAt());
    }

    public static StudentEntity toEntity(Student param) {
        if (param == null) {
            return null;
        }

        String family = JsonMapper.toJson(param.getFamily());

        return StudentEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .admissionNumber(param.getAdmissionNumber())
                .givenNames(param.getGivenNames())
                .familyName(param.getFamilyName())
                .dateOfBirth(param.getDateOfBirth())
                .admissionDate(param.getAdmissionDate())
                .enrollmentType(param.getEnrollmentType())
                .previousSchool(param.getPreviousSchool())
                .nationality(param.getNationality())
                .family(family)
                .religion(param.getReligion())
                .photo(param.getPhoto())
                .city(param.getCity())
                .street(param.getStreet())
                .gender(param.getGender())
                .session(ClassSessionMapper.toEntity(param.getSession()))
                .house(HouseMapper.toEntity(param.getHouse()))
                .parent(ParentMapper.toEntity(param.getParent()))
                .status(param.getStatus())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
