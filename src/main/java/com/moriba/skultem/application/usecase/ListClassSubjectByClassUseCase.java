package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassSubjectMapper;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassSubjectByClassUseCase {

    private final ClassSubjectRepository repo;
    private final AcademicYearRepository academicYearRepo;
    private final ClassSessionRepository classSessionRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;

    public Page<ClassSubjectDTO> execute(String school, String classId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findAllByClassIdAndSchoolId(classId, school, pageable).map(e -> {
            var academic = academicYearRepo.findActiveBySchool(school)
                    .orElseThrow(() -> new NotFoundException("active academic not found"));
            var session = classSessionRepo.findByClassIdAndAcademicYearIdAndSchoolId(classId, academic.getId(), school)
                    .orElseThrow(() -> new NotFoundException("school not found"));
            var teacher = teacherSubjectRepo
                    .findBySubjectIdAndSessionIdAndSchoolId(e.getSubject().getId(), session.getId(), school)
                    .orElse(null);
                    
            return ClassSubjectMapper.toDTO(e, teacher);
        });
    }
}
