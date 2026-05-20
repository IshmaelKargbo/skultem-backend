package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateFeeStructureDTO(

                String classId,

                @Size(max = 500, message = "Too many students selected") List<@NotBlank(message = "Student id cannot be blank") String> studentIds,

                @NotBlank(message = "Fee category is required") String feeCategory,

                @NotBlank(message = "Term is required") String termId,

                @NotNull(message = "Amount is required") @Positive(message = "Amount must be greater than zero") BigDecimal amount,

                @NotNull(message = "Due date is required") @FutureOrPresent(message = "Due date cannot be in the past") LocalDate dueDate,

                boolean allowInstallment,

                boolean hasSupply,

                @NotBlank(message = "Type is required") @Pattern(regexp = "ALL|CLASS|SELECTION", message = "Type must be ALL, SELECTION, CLASS") String type,

                int totalSupply,

                @Length(max = 255, message = "Description must not exceed 255 characters") String description) {

        public CreateFeeStructureDTO {

                // prevent both classId and studentIds
                if (classId != null
                                && !classId.isBlank()
                                && studentIds != null
                                && !studentIds.isEmpty()) {

                        throw new IllegalArgumentException(
                                        "Cannot provide both classId and studentIds");
                }

                // validate supply quantity
                if (hasSupply && totalSupply <= 0) {
                        throw new IllegalArgumentException(
                                        "Total supply must be greater than zero when hasSupply is true");
                }

                // prevent negative supply
                if (!hasSupply && totalSupply < 0) {
                        throw new IllegalArgumentException(
                                        "Total supply cannot be negative");
                }
        }
}