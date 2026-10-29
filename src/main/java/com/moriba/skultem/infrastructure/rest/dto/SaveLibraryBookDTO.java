package com.moriba.skultem.infrastructure.rest.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveLibraryBookDTO(
        @NotBlank(message = "Title is required") @Length(max = 255, message = "Title must not exceed 255 characters") String title,
        @NotBlank(message = "Author is required") @Length(max = 255, message = "Author must not exceed 255 characters") String author,
        @Length(max = 32, message = "ISBN must not exceed 32 characters") String isbn,
        @Length(max = 255, message = "Publisher must not exceed 255 characters") String publisher,
        @Length(max = 50, message = "Shelf must not exceed 50 characters") String shelf,
        @NotBlank(message = "Category is required") String categoryId,
        @NotNull(message = "Total copies is required") @Min(value = 1, message = "At least 1 copy is required") @Max(value = 100000, message = "Total copies is too large") Integer totalCopies) {
}
