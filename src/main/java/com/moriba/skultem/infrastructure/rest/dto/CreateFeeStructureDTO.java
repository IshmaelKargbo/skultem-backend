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
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateFeeStructureDTO(

        String classId,

        @Size(max = 500, message = "Too many students selected")
        List<
                @NotBlank(message = "Student id cannot be blank")
                String> studentIds,

        @NotBlank(message = "Fee category is required")
        String feeCategory,

        @NotBlank(message = "Term is required")
        String termId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Due date is required")
        @FutureOrPresent(message = "Due date cannot be in the past")
        LocalDate dueDate,

        boolean allowInstallment,

        boolean hasSupply,

        @NotBlank(message = "Type is required")
        @Pattern(
                regexp = "ALL|CLASS|SELECTION",
                message = "Type must be ALL, CLASS or SELECTION")
        String type,

        @PositiveOrZero(message = "Total supply cannot be negative")
        int totalSupply,

        String materialId,

        @Length(
                max = 255,
                message = "Description must not exceed 255 characters")
        String description

) {

    public CreateFeeStructureDTO {
        classId = normalize(classId);
        materialId = normalize(materialId);
        description = normalize(description);

        switch (type) {

            case "CLASS" -> {
                if (classId == null) {
                    throw new IllegalArgumentException(
                            "classId is required when type is CLASS");
                }
            }

            case "SELECTION" -> {
                if (studentIds == null || studentIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "studentIds are required when type is SELECTION");
                }
            }

            case "ALL" -> {
                if (classId != null) {
                    throw new IllegalArgumentException(
                            "classId is not allowed when type is ALL");
                }

                if (studentIds != null && !studentIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "studentIds are not allowed when type is ALL");
                }
            }

            default -> throw new IllegalArgumentException(
                    "Invalid type");
        }

        if (classId != null
                && studentIds != null
                && !studentIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "Cannot provide both classId and studentIds");
        }

        if (hasSupply) {

            if (materialId == null) {
                throw new IllegalArgumentException(
                        "materialId is required when hasSupply is true");
            }

            if (totalSupply <= 0) {
                throw new IllegalArgumentException(
                        "totalSupply must be greater than zero");
            }

        } else {

            if (totalSupply > 0) {
                throw new IllegalArgumentException(
                        "totalSupply must be zero when hasSupply is false");
            }

            if (materialId != null) {
                throw new IllegalArgumentException(
                        "materialId is not allowed when hasSupply is false");
            }
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}