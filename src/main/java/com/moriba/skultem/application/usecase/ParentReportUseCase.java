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
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.mapper.ParentMapper;
import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ParentReportUseCase {

    private final ParentRepository repo;
    private final @Lazy GetFeeDetailUsecase getFeeDetailUsecase;
    private final ListStudentByParentUseCase listStudentByParentUseCase;

    public Page<ParentDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();

        List<Filter> filters = request.filters();

        Page<Parent> res = repo.runReport(
                request.schoolId(),
                filters,
                pageable);

        return res.map(parent -> {
            BigDecimal totalExpected = BigDecimal.ZERO;
            BigDecimal totalCollected = BigDecimal.ZERO;
            BigDecimal totalOutstanding = BigDecimal.ZERO;
            String status = "";

            List<StudentDTO> students = listStudentByParentUseCase.execute(request.schoolId(), parent.getUser().getId(), 0, 0)
                    .getContent();

            for (StudentDTO student : students) {

                FeeDetail detail = getFeeDetailUsecase.execute(request.schoolId(), student.id());

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