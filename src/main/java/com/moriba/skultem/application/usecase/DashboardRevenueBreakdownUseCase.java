package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.RevenueBreakdownDTO;
import com.moriba.skultem.domain.model.FeeCategoryRevenue;
import com.moriba.skultem.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardRevenueBreakdownUseCase {

    private final PaymentRepository paymentRepo;

    public List<RevenueBreakdownDTO> getRevenueBreakdown(String schoolId) {
        List<FeeCategoryRevenue> categorySums = paymentRepo.sumRevenueByCategory(schoolId);

        BigDecimal totalCollected = categorySums.stream()
                .map(FeeCategoryRevenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return categorySums.stream()
                .map(s -> {
                    BigDecimal percent = BigDecimal.ZERO;
                    if (totalCollected.compareTo(BigDecimal.ZERO) > 0) {
                        percent = s.getAmount()
                                .divide(totalCollected, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    }

                    return new RevenueBreakdownDTO(
                            s.getCategory(),
                            s.getAmount().intValue(),
                            percent.setScale(0, RoundingMode.HALF_UP).intValue());
                })
                .toList();
    }
}