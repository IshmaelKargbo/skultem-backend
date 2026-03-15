package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.mapper.TeacherSubjectMapper;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SubjectReportUseCase {

    private final TeacherSubjectRepository repo;

    public Page<TeacherSubjectDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();

        List<Filter> filters = request.filters();

        Page<TeacherSubject> res = repo.runReport(
                request.schoolId(),
                filters,
                pageable
        );

        return res.map(e -> TeacherSubjectMapper.toDTO(e));
    }
}