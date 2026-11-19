package com.moriba.skultem.infrastructure.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moriba.skultem.application.services.StudentFollowUpService;
import com.moriba.skultem.application.services.StudentFollowUpService.FollowUpDTO;
import com.moriba.skultem.infrastructure.rest.dto.ApiResponse;
import com.moriba.skultem.infrastructure.security.SectionScoped;

import lombok.RequiredArgsConstructor;

// Follow-ups on pupils flagged as needing attention, per class. The class's class master and subject teachers
// read and record them (as they see the class roster and its attention flags); management always can.
@RestController
@RequestMapping("/api/v1/student-follow-up")
@RequiredArgsConstructor
public class StudentFollowUpController {

    private static final String CAN = "@permissionService.canViewClassRoster(#school, #classId) "
            + "and @sectionScope.clazz(#school, #classId)";

    private final StudentFollowUpService service;

    public record RecordDTO(String studentId, String action, String note, Boolean resolved) {
    }

    @SectionScoped
    @GetMapping("/class/{classId}")
    @PreAuthorize(CAN)
    public ApiResponse<List<FollowUpDTO>> list(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @PathVariable String classId) {
        return new ApiResponse<>("success", 200, "Follow-ups fetched successfully",
                service.listForClass(school, classId));
    }

    @SectionScoped
    @PostMapping("/class/{classId}")
    @PreAuthorize(CAN)
    public ApiResponse<FollowUpDTO> record(
            @AuthenticationPrincipal(expression = "activeSchoolId") String school,
            @AuthenticationPrincipal(expression = "userId") String userId,
            @PathVariable String classId,
            @RequestBody RecordDTO body) {
        if (body == null) {
            throw new com.moriba.skultem.application.error.BadRequestException("Nothing to record");
        }
        var res = service.record(school, classId, body.studentId(), body.action(), body.note(),
                Boolean.TRUE.equals(body.resolved()), userId);
        return new ApiResponse<>("success", 200, "Follow-up recorded", res);
    }
}
