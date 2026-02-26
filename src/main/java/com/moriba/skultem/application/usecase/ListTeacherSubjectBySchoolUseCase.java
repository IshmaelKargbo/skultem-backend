package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherSubjectMapper;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListTeacherSubjectBySchoolUseCase {

    private final TeacherSubjectRepository repo;
    private final AcademicYearRepository academicYearRepo;

    public Page<TeacherSubjectDTO> execute(String school, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        var academicYear = academicYearRepo.findActiveBySchool(school)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

        return repo.findAllBySchoolIdAndAcademicYearId(school, academicYear.getId(), pageable)
                .map(TeacherSubjectMapper::toDTO);
    }
}
