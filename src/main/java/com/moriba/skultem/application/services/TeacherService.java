package com.moriba.skultem.application.services;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.application.usecase.EditTeacherUseCase;
import com.moriba.skultem.application.usecase.ResolveAcademicYearUseCase;
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
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final EditTeacherUseCase teacherUseCase;

    // Whitelisted rather than handed straight to Sort.by(sortBy) - this ends up as a JPQL "order by
    // u.<field>"/"t.<field>", so an unchecked client value would let someone probe/sort by
    // arbitrary entity fields. See ListFeeStructureBySchoolUseCase for the same pattern.
    private static final Set<String> SORTABLE_FIELDS = Set.of("user.givenName", "user.familyName", "createdAt");

    public Page<TeacherDTO> search(String search, int page, int size, String schoolId) {
        return search(search, page, size, schoolId, null, null);
    }

    public Page<TeacherDTO> search(String search, int page, int size, String schoolId, String sortBy,
            String direction) {
        Sort sort = resolveSort(sortBy, direction);
        Pageable pageable = (size > 0)
                ? PageRequest.of(page, size, sort)
                : Pageable.unpaged(sort);

        return repo.search(search, schoolId, pageable)
                .map(TeacherMapper::toDTO);
    }

    private Sort resolveSort(String sortBy, String direction) {
        String field = (sortBy != null && SORTABLE_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    public TeacherDTO getById(String id, String academicYearId) {
        return repo.findById(id)
                .map((e) -> {
                    var classes = getClasses(e.getId(), e.getSchoolId(), academicYearId);
                    return TeacherMapper.toDTO(e, classes);
                })
                .orElseThrow(() -> new NotFoundException("Teacher not found"));
    }

    public TeacherDTO edit(TeacherRecord dto) {
        return teacherUseCase.execute(dto.schoolId(), dto.teacherId(), dto.title(), dto.givenNames(), dto.familyName(),
                dto.gender(), dto.staffId(), dto.phone(), dto.street(), dto.city(), dto.designation());
    }

    public record TeacherRecord(String schoolId, String teacherId, Title title, String givenNames, String familyName,
            Gender gender, String staffId, String phone, String street, String city, String designation) {
    }

    private List<String> getClasses(String teacherId, String schoolId, String academicYearId) {

        var academic = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        var classes = classMasterRepo
                .findByTeacherAndAcademicYear(teacherId, academic.getId(), Pageable.unpaged());

        return classes.getContent()
                .stream()
                .map(cm -> cm.getSession().getClazz().getName())
                .toList();
    }
}