package com.moriba.skultem.application.usecase;

import java.util.Objects;

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

    public Page<ClassSubjectDTO> execute(String school, String classId, String stream, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        if (stream != null && !stream.isEmpty())
            return repo.findAllByClassIdAndStreamIdAndSchoolId(classId, stream, school, pageable).map(e -> {
                var academic = academicYearRepo.findActiveBySchool(school)
                        .orElseThrow(() -> new NotFoundException("active academic not found"));
                var session = classSessionRepo
                        .findByClassIdAndStreamIdAndAcademicYearId(classId, stream, academic.getId())
                        .orElseThrow(() -> new NotFoundException("session not found"));
                var teacher = teacherSubjectRepo
                        .findBySubjectIdAndSessionIdAndSchoolId(e.getSubject().getId(), session.getId(), school)
                        .orElse(null);

                return ClassSubjectMapper.toDTO(e, teacher);
            });

        return repo.findAllByClassIdAndSchoolId(classId, school, pageable).map(e -> {
            var academic = academicYearRepo.findActiveBySchool(school)
                    .orElseThrow(() -> new NotFoundException("active academic not found"));
            // A class with streams (e.g. SSS's Science/Arts/Commercial) has one session per
            // stream for the same class+year, so "the" session isn't unique without a stream to
            // narrow by - findByClassIdAndAcademicYearIdAndSchoolId used to assume a single
            // result and threw IncorrectResultSizeDataAccessException for any such class,
            // failing this whole listing. Look across every session for the class instead, and
            // just leave the teacher unresolved if none of them has one - it's supplementary
            // info here, not something worth failing the subject list over.
            var sessions = classSessionRepo.findAllByClassIdAndAcademicYearIdAndSchoolId(classId, academic.getId(),
                    school);
            var teacher = sessions.stream()
                    .map(session -> teacherSubjectRepo
                            .findBySubjectIdAndSessionIdAndSchoolId(e.getSubject().getId(), session.getId(), school)
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            return ClassSubjectMapper.toDTO(e, teacher);
        });
    }
}
