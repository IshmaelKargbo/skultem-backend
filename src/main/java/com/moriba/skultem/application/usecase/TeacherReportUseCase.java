package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.TeacherDTO;
import com.moriba.skultem.application.mapper.TeacherMapper;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TeacherReportUseCase {

    private final TeacherRepository repo;

    public Page<TeacherDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = createPageable(page, size);

        List<Filter> filters = request.filters();

        Page<Teacher> enrollments = repo.runReport(
                request.schoolId(),
                filters,
                pageable
        );

        return enrollments.map(e -> TeacherMapper.toDTO(e));
    }

    private Pageable createPageable(int page, int size) {

        if (size <= 0) {
            return Pageable.unpaged();
        }

        int pageNumber = Math.max(page - 1, 0);

        return PageRequest.of(pageNumber, size);
    }
}