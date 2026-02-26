package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.SectionRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PromoteClassUseCase {
    private final StudentRepository studentRepo;
    private final ClassRepository classRepo;
    private final SectionRepository sectionRepo;
    private final AcademicYearRepository academicYearRepo;

    public void execute(String schoolId, String classId) {
        // var academicYear = academicYearRepo.findActiveBySchool(schoolId)
        //         .orElseThrow(() -> new RuleException("active academic year not found"));
        // if (academicYear.getEndDate().isAfter(LocalDate.now())) {
        //     throw new RuleException("promotion only allowed when academic year ends");
        // }

        // var schoolClass = classRepo.findById(classId).orElseThrow(() -> new RuleException("class not found"));
        // if (!schoolClass.getSchoolId().equals(schoolId)) {
        //     throw new RuleException("class not in school");
        // }

        // var students = studentRepo.findByClass(classId, Pageable.unpaged()).getContent();
        // for (Student student : students) {
        //     if (academicYear.getId().equals(student.getLastPromotedAcademicYearId())) {
        //         throw new RuleException("student already promoted in this academic year");
        //     }
        // }

        // var nextClass = schoolClass.getNextClass() != null
        //         ? classRepo.findById(schoolClass.getNextClass()).orElse(null)
        //         : classRepo.findBySchoolAndLevelOrder(schoolId, schoolClass.getDisplayOrder() + 1).orElse(null);

        // for (Student student : students) {
        //     if (nextClass == null) {
        //         student.markGraduated(academicYear.getId());
        //         studentRepo.save(student);
        //         continue;
        //     }
        //     String nextSectionId = sectionRepo.findByClassAndName(nextClass.getId(),
        //             sectionRepo.findById(student.getSectionId()).map(s -> s.getName()).orElse(""))
        //             .map(sec -> sec.getId())
        //             .orElseGet(() -> sectionRepo.findByClass(nextClass.getId(), Pageable.unpaged())
        //                     .getContent()
        //                     .stream()
        //                     .findFirst()
        //                     .map(sec -> sec.getId())
        //                     .orElse(student.getSectionId()));

        //     String nextStreamId = nextClass.getLevel() == Level.SSS ? student.getStreamId() : null;
        //     student.promoteTo(nextClass.getId(), nextSectionId, nextStreamId, academicYear.getId());
        //     studentRepo.save(student);
        // }
    }
}
