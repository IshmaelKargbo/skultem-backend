package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SchemeOfWorkMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.WeekRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Flips a scheme of work between DRAFT (still being built - hidden from a parent's Curriculum
// view) and PUBLISH (finished - a parent's GetChildCurriculumUseCase only ever returns PUBLISH
// schemes). A teacher already sees their own DRAFT schemes via GET /scheme/me regardless of
// state - this only changes what the school makes visible to families.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSchemeStateUseCase {

    private final SchemeOfWorkRepository repo;
    private final WeekRepository weekRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SCHEME_STATE_UPDATED")
    public SchemeOfWorkDTO execute(String schoolId, String schemeId, SchemeOfWork.State state) {
        var scheme = repo.findById(schemeId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new NotFoundException("Scheme of work not found"));

        scheme.setState(state);
        repo.save(scheme);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                state == SchemeOfWork.State.PUBLISH ? "Scheme of work published" : "Scheme of work unpublished",
                scheme.getSubject().getName() + " - " + scheme.getSession().getName(),
                null,
                scheme.getId());

        var progress = Week.deriveProgress(weekRepo.findAllByScheme(scheme.getId()).stream()
                .map(Week::getState).toList());

        return SchemeOfWorkMapper.toDTO(scheme, progress);
    }
}
