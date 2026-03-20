package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentByParentUseCase {
        private final StudentRepository repo;
        private final EnrollmentRepository enrollmentRepo;
        private final ParentRepository parentRepo;
        private final ClassMasterRepository classMasterRepo;

        public Page<StudentDTO> execute(String schoolId, String parentId, int page, int size) {
                Pageable pageable = Pageable.unpaged();
                if (size > 0) {
                        pageable = PageRequest.of(page - 1, size);
                }

                var parent = parentRepo.findByUserIdAndSchoolId(parentId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Parent not found"));

                return repo.findByParentAndSchoolId(parent.getId(), schoolId, pageable).map(e -> {
                        var session = e.getSession();
                        var classMaster = classMasterRepo.findBySessionIdAndSchoolId(e.getSession().getId(), schoolId)
                                        .orElseThrow(() -> new NotFoundException("Class master not found"));
                        var teacher = classMaster.getTeacher().getName();

                        var enrollment = enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(e.getId(),
                                        e.getSession().getAcademicYear().getId(), schoolId)
                                        .orElseThrow(() -> new NotFoundException("Class master not found"));

                        List<Filter> filters = new ArrayList<Filter>();
                        filters.add(new Filter("clazz.id", FilterOperator.EQUALS, "select",
                                        session.getClazz().getId(), null,
                                        null));
                        filters.add(new Filter("academicYear.id", FilterOperator.EQUALS, "select",
                                        session.getAcademicYear().getId(), null,
                                        null));

                        var classSize = enrollmentRepo.runReport(schoolId, filters, Pageable.unpaged()).getSize();

                        teacher = classMaster.getTeacher().getName();

                        return StudentMapper.toDTO(e, enrollment, teacher, classSize, session.getId());
                });
        }
}
