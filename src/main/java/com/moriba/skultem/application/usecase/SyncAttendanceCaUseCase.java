package com.moriba.skultem.application.usecase;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.application.services.AttendanceCaService;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.infrastructure.security.PermissionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Brings the attendance-based CA part up to date from the register. Does nothing for an assessment without such a
// part, or once the CA has been submitted (the marks are then frozen). Returns how many students' marks changed.
@Service
@Transactional
@RequiredArgsConstructor
public class SyncAttendanceCaUseCase {

    private final TeacherSubjectRepository teacherSubjectRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final AssessmentCaEntryRepository caEntryRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final AssessmentStructureService structureService;
    private final AttendanceCaService attendanceCaService;

    public int execute(String schoolId, String teacherSubjectId, String assessmentId, String termId) {
        var ts = teacherSubjectRepo.findByIdAndSchoolId(teacherSubjectId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher subject not found"));
        var session = ts.getSession();

        var scores = new ArrayList<AssessmentScore>();
        for (var sa : studentAssessmentRepo.findAllBySubjectAndSessionAndTermId(ts.getSubject().getId(),
                session.getId(), termId)) {
            scores.addAll(assessmentScoreRepo.findAllByStudentAssessmentIdAndAssessmentId(sa.getId(), assessmentId));
        }
        if (scores.isEmpty()) {
            return 0;
        }

        var cycles = scores.stream().map(AssessmentScore::getCycle).distinct().toList();
        cycleRepo.saveAll(structureService.freezeOpened(cycles));
        var cycle = cycles.get(0);
        if (!cycle.isContinuous() || cycle.attendanceEntryNumber() == 0 || !cycle.canEdit() || cycle.isCaSubmitted()) {
            return 0;
        }

        var userId = PermissionService.getCurrentUser().userId();
        var result = attendanceCaService.sync(schoolId, ts, cycle, scores, userId);
        caEntryRepo.saveAll(result.entries());
        assessmentScoreRepo.saveAll(result.scores());
        return result.scores().size();
    }
}
