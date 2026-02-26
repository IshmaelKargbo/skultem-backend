package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeDiscountDTO;
import com.moriba.skultem.domain.model.FeeDiscount;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.utils.MoneyUtil;

@Service
@Transactional
@RequiredArgsConstructor
public class ListFeeDiscountBySchoolUseCase {
    private final FeeDiscountRepository repo;

    public Page<FeeDiscountDTO> execute(String schoolId, int page, int size) {

        Pageable pageable = (size > 0)
                ? PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                : Pageable.unpaged();

        Page<FeeDiscount> discounts = repo.findAllBySchool(schoolId, pageable);

        return discounts.map(discount -> {
            Student student = discount.getEnrollment().getStudent();
            String name = String.join(" ", student.getGivenNames(), student.getFamilyName());
            String totalSavedString = MoneyUtil.format(discount.computeSavings());

            return new FeeDiscountDTO(
                    discount.getId(),
                    discount.getName(),
                    discount.getReason(),
                    name,
                    discount.getClazz(),
                    discount.getKind().name(),
                    discount.value(),
                    discount.appliedDate(),
                    discount.getExpiryDate(),
                    totalSavedString);
        });
    }
}