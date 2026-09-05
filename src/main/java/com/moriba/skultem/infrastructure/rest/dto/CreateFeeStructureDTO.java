package com.moriba.skultem.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateFeeStructureDTO(

        // A CLASS-type fee can now target several classes in one request - e.g. Class 1 and Class
        // 2 sharing the same Tuition amount - by creating one FeeStructure per class behind the
        // scenes (see CreateFeeStructureUseCase). Each class still gets its own independent
        // FeeStructure row (so editing/deleting one later never touches the others); this is only
        // a bulk-create convenience, not a shared record.
        @Size(max = 50, message = "Too many classes selected")
        List<
                @NotBlank(message = "Class id cannot be blank")
                String> classIds,

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

        // Only ever charged the first time a student enrolls (e.g. Uniform Fee) - never to a
        // student who's simply continuing/being promoted/re-enrolled into what this fee targets.
        // Doesn't apply to type SELECTION - an explicit student list is already a deliberate,
        // one-off assignment regardless of new/existing status.
        boolean newStudentsOnly,

        // Only ever charged to a student whose overall admission is a re-enrollment (a returning
        // student) - never a first-time NEW/TRANSFER admission. Mutually exclusive with
        // newStudentsOnly, and doesn't apply to type SELECTION for the same reason newStudentsOnly
        // doesn't - an explicit student list is already a deliberate, one-off assignment.
        boolean oldStudentsOnly,

        // Null reaches every gender. Set when a fee only applies to one - most commonly a supply
        // fee (hasSupply) priced differently for boys vs girls (e.g. two Uniform fees, same
        // category/term/class, one MALE and one FEMALE) - but not restricted to supply fees only.
        @Pattern(regexp = "MALE|FEMALE", message = "Gender must be MALE or FEMALE")
        String gender,

        boolean hasSupply,

        @NotBlank(message = "Type is required")
        @Pattern(
                regexp = "ALL|CLASS|SELECTION",
                message = "Type must be ALL, CLASS or SELECTION")
        String type,

        // One or more materials this fee bundles when hasSupply is true - e.g. a Uniform fee
        // carrying the Uniform itself, a House Colour, and a Necktie, each its own line.
        @Valid
        List<FeeStructureSupplyItemInputDTO> supplyItems,

        @Length(
                max = 255,
                message = "Description must not exceed 255 characters")
        String description

) {

    public CreateFeeStructureDTO {
        description = normalize(description);
        gender = normalize(gender);

        switch (type) {

            case "CLASS" -> {
                if (classIds == null || classIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "At least one class is required when type is CLASS");
                }
            }

            case "SELECTION" -> {
                if (studentIds == null || studentIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "studentIds are required when type is SELECTION");
                }

                if (newStudentsOnly) {
                    throw new IllegalArgumentException(
                            "newStudentsOnly is not allowed when type is SELECTION");
                }

                if (oldStudentsOnly) {
                    throw new IllegalArgumentException(
                            "oldStudentsOnly is not allowed when type is SELECTION");
                }

                if (gender != null) {
                    throw new IllegalArgumentException(
                            "gender is not allowed when type is SELECTION - an explicit student list is already a deliberate assignment");
                }
            }

            case "ALL" -> {
                if (classIds != null && !classIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "classIds are not allowed when type is ALL");
                }

                if (studentIds != null && !studentIds.isEmpty()) {
                    throw new IllegalArgumentException(
                            "studentIds are not allowed when type is ALL");
                }
            }

            default -> throw new IllegalArgumentException(
                    "Invalid type");
        }

        if (newStudentsOnly && oldStudentsOnly) {
            throw new IllegalArgumentException(
                    "newStudentsOnly and oldStudentsOnly cannot both be true");
        }

        if (classIds != null
                && !classIds.isEmpty()
                && studentIds != null
                && !studentIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "Cannot provide both classIds and studentIds");
        }

        if (hasSupply) {

            if (supplyItems == null || supplyItems.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one supply item is required when hasSupply is true");
            }

        } else {

            if (supplyItems != null && !supplyItems.isEmpty()) {
                throw new IllegalArgumentException(
                        "supplyItems are not allowed when hasSupply is false");
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
