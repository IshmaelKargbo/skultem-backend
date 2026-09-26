package com.moriba.skultem.infrastructure.rest;

import com.moriba.skultem.infrastructure.security.SectionNeutral;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import com.moriba.skultem.domain.vo.FeatureModule;
import com.moriba.skultem.infrastructure.security.RequiresModule;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.dto.NoticeDTO;
import com.moriba.skultem.application.usecase.CreateNoticeUseCase;
import com.moriba.skultem.application.usecase.DeleteNoticeUseCase;
import com.moriba.skultem.application.usecase.GetNoticeUseCase;
import com.moriba.skultem.application.usecase.ListNoticeBySchoolUseCase;
import com.moriba.skultem.application.usecase.TogglePinNoticeUseCase;
import com.moriba.skultem.application.usecase.UpdateNoticeUseCase;
import com.moriba.skultem.domain.model.Notice.Category;
import com.moriba.skultem.domain.vo.Audience;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.rest.dto.CreateNoticeDTO;
import com.moriba.skultem.infrastructure.rest.dto.UpdateNoticeDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiresModule(FeatureModule.COMMUNICATION)
@RestController
@RequestMapping("/api/v1/notice")
@RequiredArgsConstructor
public class NoticeController {
    private final CreateNoticeUseCase createNoticeUseCase;
    private final GetNoticeUseCase getNoticeUseCase;
    private final ListNoticeBySchoolUseCase listNoticeBySchoolUseCase;
    private final UpdateNoticeUseCase updateNoticeUseCase;
    private final DeleteNoticeUseCase deleteNoticeUseCase;
    private final TogglePinNoticeUseCase togglePinNoticeUseCase;

    // Posting is open to a section-limited Admin/Teacher too: the notice is then for their own section only
    // (CommunicationScopeService#resolveTarget) and only that section's people see it.
    @SectionScoped
    @PostMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER')")
    public ApiResponse<NoticeDTO> create(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @Valid @RequestBody CreateNoticeDTO param) {
        var category = Category.valueOf(param.category());
        var audience = Audience.valueOf(param.audience());
        var res = createNoticeUseCase.execute(school, userId, param.title(), param.content(), category, audience,
                param.expiresAt(), param.eventAt(), param.eventEndsAt(), param.eventLocation(),
                Boolean.TRUE.equals(param.addToCalendar()), param.managementSectionId());
        return new ApiResponse<>("success", 200, "Notice posted successfully", res);
    }

    @SectionNeutral
    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT', 'ACCOUNTANT')")
    public ApiResponse<NoticeDTO> get(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = getNoticeUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Notice fetched successfully", res);
    }

    @SectionNeutral
    @GetMapping
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER', 'PARENT', 'ACCOUNTANT')")
    public ApiResponse<List<NoticeDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @RequestParam(required = true, defaultValue = "10") Integer size,
            @RequestParam(required = true, defaultValue = "1") Integer page) {

        var res = listNoticeBySchoolUseCase.execute(school, page, size);
        var list = res.getContent();
        Map<String, Object> meta = Map.of(
                "page", res.getNumber() + 1,
                "size", res.getSize(),
                "count", res.getTotalElements(),
                "pages", res.getTotalPages());

        return new ApiResponse<>("success", 200, "Notices fetched successfully", list, meta);
    }

    @SectionScoped
    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.notice(#school, #id)")
    public ApiResponse<NoticeDTO> update(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id,
            @Valid @RequestBody UpdateNoticeDTO param) {
        var category = Category.valueOf(param.category());
        var audience = Audience.valueOf(param.audience());
        var res = updateNoticeUseCase.execute(school, id, param.title(), param.content(), category, audience,
                param.expiresAt(), param.eventAt(), param.eventEndsAt(), param.eventLocation(),
                Boolean.TRUE.equals(param.addToCalendar()));
        return new ApiResponse<>("success", 200, "Notice updated successfully", res);
    }

    @SectionScoped
    @PatchMapping("/{id}/pin")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR', 'TEACHER') and @sectionScope.notice(#school, #id)")
    public ApiResponse<NoticeDTO> togglePin(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        var res = togglePinNoticeUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Notice updated successfully", res);
    }

    @SectionScoped
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasAnySchoolRole(#school, 'ADMIN', 'OWNER', 'PROPRIETOR') and @sectionScope.notice(#school, #id)")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String id) {
        deleteNoticeUseCase.execute(school, id);
        return new ApiResponse<>("success", 200, "Notice deleted successfully", null);
    }
}
