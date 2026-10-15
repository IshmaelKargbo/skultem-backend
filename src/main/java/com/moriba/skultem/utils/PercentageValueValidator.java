package com.moriba.skultem.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.moriba.skultem.infrastructure.rest.dto.CreateFeeDiscountDTO;

import java.math.BigDecimal;

public class PercentageValueValidator implements ConstraintValidator<ValidPercentageValue, CreateFeeDiscountDTO> {

    @Override
    public boolean isValid(CreateFeeDiscountDTO dto, ConstraintValidatorContext context) {
        if (dto.kind() == null || dto.fees() == null) return true; // skip other null checks
        if ("PERCENTAGE".equals(dto.kind())) {
            return dto.fees().stream().allMatch(f -> f == null || f.value() == null
                    || (f.value().compareTo(BigDecimal.ONE) >= 0 && f.value().compareTo(BigDecimal.valueOf(100)) <= 0));
        }
        return true;
    }
}