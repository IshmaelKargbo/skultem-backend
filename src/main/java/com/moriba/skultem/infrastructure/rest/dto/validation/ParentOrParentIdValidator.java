package com.moriba.skultem.infrastructure.rest.dto.validation;

import com.moriba.skultem.infrastructure.rest.dto.CreateStudentDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class ParentOrParentIdValidator
        implements ConstraintValidator<ValidParentOrParentId, CreateStudentDTO> {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Override
    public boolean isValid(
            CreateStudentDTO dto,
            ConstraintValidatorContext context
    ) {

        if (dto == null) {
            return true;
        }

        boolean hasParentId =
                dto.parentId() != null &&
                !dto.parentId().isBlank();

        boolean hasParent =
                dto.parent() != null;

        // Must provide one
        if (!hasParentId && !hasParent) {

            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                    "Parent details are required"
            ).addPropertyNode("parent")
             .addConstraintViolation();

            return false;
        }

        // If parentId exists, skip parent validation
        if (hasParentId) {
            return true;
        }

        // Validate parent object manually
        var violations = validator.validate(dto.parent());

        if (!violations.isEmpty()) {

            context.disableDefaultConstraintViolation();

            violations.forEach(v ->
                    context.buildConstraintViolationWithTemplate(
                                    v.getMessage()
                            )
                            .addPropertyNode(
                                    "parent." + v.getPropertyPath()
                            )
                            .addConstraintViolation()
            );

            return false;
        }

        return true;
    }
}