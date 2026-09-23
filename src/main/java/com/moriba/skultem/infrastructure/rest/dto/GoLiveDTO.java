package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

import com.moriba.skultem.domain.model.PlaygroundDataCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Taking a playground school live - see WipeTestSchoolDataUseCase. An empty list keeps every
// record; `confirmation` must be the school's domain.
public record GoLiveDTO(
        @NotNull(message = "Choose which data to clear (an empty list keeps everything)") List<PlaygroundDataCategory> categories,

        @NotBlank(message = "Type the school's domain to confirm") String confirmation
) {
}
