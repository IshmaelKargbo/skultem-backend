package com.moriba.skultem.application.usecase;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.TransactionDTO;
import com.moriba.skultem.application.mapper.TransactionMapper;
import com.moriba.skultem.domain.repository.TransactionRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionReportUseCase {

        private final TransactionRepository repo;

        public Page<TransactionDTO> execute(ReportBuilderDTO request, int page, int size) {

                Pageable pageable = (size > 0)
                                ? PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                                : Pageable.unpaged();

                List<Filter> filters = request.filters();

                return repo.runReport(request.schoolId(), filters, pageable).map(TransactionMapper::toDTO);
        }
}