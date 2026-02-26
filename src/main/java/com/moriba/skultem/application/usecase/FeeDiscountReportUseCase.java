package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeDiscountReportDTO;
import com.moriba.skultem.domain.model.FeeDiscount;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.utils.MoneyUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class FeeDiscountReportUseCase {

        private final FeeDiscountRepository repo;

        public FeeDiscountReportDTO calculateReport(String schoolId) {

                List<FeeDiscount> discounts = repo.findAllBySchool(schoolId, Pageable.unpaged()).getContent();

                long activeCount = discounts.stream()
                                .filter(FeeDiscount::isActive)
                                .count();

                long expiredCount = discounts.stream()
                                .filter(d -> !d.isActive())
                                .count();

                BigDecimal totalSavings = discounts.stream()
                                .filter(FeeDiscount::isActive)
                                .map(FeeDiscount::computeSavings)
                                .reduce(BigDecimal.ZERO, (a, b) -> a.add((BigDecimal) b));

                return new FeeDiscountReportDTO(activeCount, MoneyUtil.format(totalSavings), expiredCount);
        }

}