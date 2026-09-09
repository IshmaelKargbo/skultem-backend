package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ChildSchemeOfWorkDTO;
import com.moriba.skultem.application.dto.SchemeProgressDTO;
import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SchemeOfWorkMapper;
import com.moriba.skultem.application.mapper.WeekMapper;
import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.repository.WeekRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// What a parent sees under Curriculum: their child's published scheme(s) of work for a given term
// - one per subject the class is taking - each with its week-by-week topic list and coverage so
// far. DRAFT schemes (still being built by the teacher) are filtered out here rather than at the
// repository level - a parent has no use for an unfinished plan, and it isn't really "published"
// information yet; the existing admin/teacher search() is left untouched.
@Service
@Transactional
@RequiredArgsConstructor
public class GetChildCurriculumUseCase {

    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final SchemeOfWorkRepository schemeRepo;
    private final WeekRepository weekRepo;

    public List<ChildSchemeOfWorkDTO> execute(String schoolId, String parentUserId, String studentId, String termId) {
        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));

        var parent = parentRepo.findByUserIdAndSchoolId(parentUserId, schoolId)
                .orElseThrow(() -> new NotFoundException("Parent not found"));

        // A parent can only see their own child's curriculum - sessionId/studentId aren't
        // otherwise scoped to the caller anywhere in this flow, so this check is what actually
        // stops one family from browsing another's child by guessing/enumerating a studentId.
        if (student.getParent() == null || !student.getParent().getId().equals(parent.getId())) {
            throw new AccessDeniedException("You can only view your own child's curriculum");
        }

        var sessionId = student.getSession().getId();

        var schemes = schemeRepo.search(schoolId, null, sessionId, termId, null, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(s -> s.getState() == SchemeOfWork.State.PUBLISH)
                .toList();

        return schemes.stream().map(this::toChildDTO).toList();
    }

    private ChildSchemeOfWorkDTO toChildDTO(SchemeOfWork scheme) {
        var weeks = weekRepo.findAllByScheme(scheme.getId()).stream()
                .sorted((a, b) -> Integer.compare(a.getWeek(), b.getWeek()))
                .toList();

        var progressState = Week.deriveProgress(weeks.stream().map(Week::getState).toList());

        int totalWeeks = weeks.size();
        int completed = (int) weeks.stream().filter(w -> w.getState() == Week.State.COMPLETED).count();
        BigDecimal coverage = totalWeeks == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalWeeks), 2, RoundingMode.HALF_UP);

        var progress = new SchemeProgressDTO(scheme.getId(), totalWeeks, completed, totalWeeks - completed, coverage);
        var weekDTOs = weeks.stream().map(WeekMapper::toDTO).toList();

        return new ChildSchemeOfWorkDTO(SchemeOfWorkMapper.toDTO(scheme, progressState), progress, weekDTOs);
    }
}
