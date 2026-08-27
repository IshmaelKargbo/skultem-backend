package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.House;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.HouseRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Randomly, but evenly, spreads every student in a class across the
 * school's active houses. Students are shuffled and then dealt out to a
 * shuffled house rotation, so each house ends up with as close to the same
 * number of students as possible while the pairing stays unpredictable.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PickBestHouse {

    private final EnrollmentRepository enrollmentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final HouseRepository houseRepo;
    private final StudentRepository studentRepo;

    @AuditLogAnnotation(action = "HOUSE_RANDOM_ASSIGNED")
    public void assignHouse(String school, String classId) {
        var academic = academicYearRepo.findActiveBySchool(school)
                .orElseThrow(() -> new NotFoundException("no active academic year found"));

        var enrollments = enrollmentRepo.findAllByClassAndAcademicAndSchoolId(classId, academic.getId(), school,
                Pageable.unpaged());

        List<Student> students = new ArrayList<>(
                enrollments.getContent().stream().map(e -> e.getStudent()).toList());

        if (students.isEmpty()) {
            throw new NotFoundException("No students found in this class");
        }

        List<House> houses = new ArrayList<>(houseRepo.findAllBySchoolId(school, Pageable.unpaged()).getContent());

        if (houses.isEmpty()) {
            throw new NotFoundException("No houses have been created for this school yet");
        }

        Collections.shuffle(students);
        Collections.shuffle(houses);

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            House house = houses.get(i % houses.size());
            student.assignHouse(house);
            studentRepo.save(student);
        }
    }
}
