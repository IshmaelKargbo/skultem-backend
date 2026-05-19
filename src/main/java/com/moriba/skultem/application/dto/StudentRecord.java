package com.moriba.skultem.application.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.vo.Family;
import com.moriba.skultem.domain.vo.Gender;

public record StudentRecord(
                String classId,
                String schoolId,
                MultipartFile photo,
                String givenNames,
                String familyName,
                String admissionNumber,
                LocalDate admissionDate,
                EnrollmentType enrollmentType,
                String previousSchool,
                String nationality,
                String religion,
                String city,
                String street,
                String lastClass,
                String house,
                Gender gender,
                Family family,
                String relationship,
                String parentId,
                ParentRequest parent,
                LocalDate dateOfBirth,
                List<String> selectedOptionIds) {
}
