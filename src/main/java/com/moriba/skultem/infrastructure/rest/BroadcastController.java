package com.moriba.skultem.infrastructure.rest;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.BroadcastDTO;
import com.moriba.skultem.application.usecase.ComposeBroadcastUseCase;
import com.moriba.skultem.application.usecase.GetAudienceSizeUseCase;
import com.moriba.skultem.application.usecase.ListBroadcastBySchoolUseCase;
import com.moriba.skultem.domain.model.Broadcast;
import com.moriba.skultem.domain.vo.Audience;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.ComposeBroadcastDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/broadcast")
@RequiredArgsConstructor
public class BroadcastController {
    private final ComposeBroadcastUseCase composeBroadcastUseCase;
    private final ListBroadcastBySchoolUseCase listBroadcastBySchoolUseCase;
    private final GetAudienceSizeUseCase getAudienceSizeUseCase;

    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<BroadcastDTO> compose(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody ComposeBroadcastDTO param) {
        var audience = Audience.valueOf(param.audience());
        var channels = param.channels().stream().map(Broadcast.Channel::valueOf).toList();
        var sendOption = Broadcast.SendOption.valueOf(param.sendOption());

        var res = composeBroadcastUseCase.execute(school, userId, param.title(), param.message(), audience, channels,
                sendOption, param.scheduledAt());

        var message = sendOption == Broadcast.SendOption.NOW ? "Broadcast sent successfully"
                : "Broadcast scheduled successfully";
        return new ApiResponse<>("success", 200, message, res);
    }

    @GetMapping("/audience-size")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR')")
    public ApiResponse<Integer> audienceSize(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam String audience) {
        var res = getAudienceSizeUseCase.execute(school, Audience.valueOf(audience));
        return new ApiResponse<>("success", 200, "Audience size fetched successfully", res);
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<List<BroadcastDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listBroadcastBySchoolUseCase.execute(school, page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Broadcasts fetched successfully", list, meta);
    }
}
