package com.moriba.skultem.application.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.application.usecase.EditTeacherUseCase;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.domain.vo.Title;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository repo;
    private final ClassMasterRepository classMasterRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EditTeacherUseCase teacherUseCase;

    public Page<TeacherDTO> search(String search, int page, int size, String schoolId) {
        Pageable pageable = (size > 0)
                ? PageRequest.of(page, size)
                : Pageable.unpaged();

        return repo.search(search, schoolId, pageable)
                .map(TeacherMapper::toDTO);
    }

    public TeacherDTO getById(String id) {
        return repo.findById(id)
                .map((e) -> {
                    var classes = getClasses(e.getId(), e.getSchoolId());
                    return TeacherMapper.toDTO(e, classes);
                })
                .orElseThrow(() -> new NotFoundException("Teacher not found"));
    }

    public TeacherDTO edit(TeacherRecord dto) {
        return teacherUseCase.execute(dto.schoolId(), dto.teacherId(), dto.title(), dto.givenNames(), dto.familyName(),
                dto.gender(), dto.staffId(), dto.phone(), dto.street(), dto.city());
    }

    public record TeacherRecord(String schoolId, String teacherId, Title title, String givenNames, String familyName,
            Gender gender, String staffId, String phone, String street, String city) {
    }

    private List<String> getClasses(String teacherId, String schoolId) {

        var academic = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("No active academic year found"));

        var classes = classMasterRepo
                .findByTeacherAndAcademicYear(teacherId, academic.getId(), Pageable.unpaged());

        return classes.getContent()
                .stream()
                .map(cm -> cm.getSession().getClazz().getName())
                .toList();
    }
}