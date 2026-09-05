package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeDetail;
import com.moriba.skultem.application.dto.ParentDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.mapper.ParentMapper;
import com.moriba.skultem.domain.repository.ParentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListParentBySchoolUseCase {

    private final ParentRepository repo;
    private final @Lazy GetFeeDetailUsecase getFeeDetailUsecase;
    private final ListStudentByParentUseCase listStudentByParentUseCase;

    public Page<ParentDTO> execute(String schoolId, int page, int size) {
        return execute(schoolId, page, size, null);
    }

    public Page<ParentDTO> execute(String schoolId, int page, int size, String query) {

        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        boolean hasQuery = query != null && !query.isBlank();
        var parents = hasQuery ? repo.search(schoolId, query.trim(), pageable) : repo.findBySchool(schoolId, pageable);

        return parents.map(parent -> {
            BigDecimal totalExpected = BigDecimal.ZERO;
            BigDecimal totalCollected = BigDecimal.ZERO;
            BigDecimal totalOutstanding = BigDecimal.ZERO;
            String status = "";

            List<StudentDTO> students = listStudentByParentUseCase.execute(schoolId, parent.getUser().getId(), 0, 0)
                    .getContent();

            for (StudentDTO student : students) {

                FeeDetail detail = getFeeDetailUsecase.execute(schoolId, student.id());

                if (detail != null) {
                    if (detail.total() != null)
                        totalExpected = totalExpected.add(detail.total());

                    if (detail.paid() != null)
                        totalCollected = totalCollected.add(detail.paid());

                    if (detail.balance() != null)
                        totalOutstanding = totalOutstanding.add(detail.balance());

                    status = detail.status();
                }
            }

            var feeDetail = new FeeDetail(totalExpected, totalCollected, totalOutstanding, status);
            return ParentMapper.toDTO(parent, feeDetail, students.size());
        });
    }
}