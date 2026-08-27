package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LessonDTO;
import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.application.dto.SchemeProgressDTO;
import com.moriba.skultem.application.dto.TeacherProgressDTO;
import com.moriba.skultem.application.dto.TeacherProgressDetailDTO;
import com.moriba.skultem.application.dto.WeekDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LessonMapper;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.application.mapper.SchemeOfWorkMapper;
import com.moriba.skultem.application.mapper.WeekMapper;
import com.moriba.skultem.application.usecase.CreateLessonUseCase;
import com.moriba.skultem.application.usecase.CreateWeekUseCase;
import com.moriba.skultem.application.usecase.GetTeacherProgressDetailUseCase;
import com.moriba.skultem.application.usecase.GetTeacherProgressUseCase;
import com.moriba.skultem.application.usecase.ManageSchemeOfWorkUseCase;
import com.moriba.skultem.application.usecase.UpdateLessonStateUseCase;
import com.moriba.skultem.application.usecase.UpdateWeekStateUseCase;
import com.moriba.skultem.domain.model.Lesson;
import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.application.usecase.ResolveAcademicYearUseCase;
import com.moriba.skultem.domain.repository.LessonRepository;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.WeekRepository;
import com.moriba.skultem.domain.vo.LessonStage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final SchemeOfWorkRepository repo;
    private final WeekRepository weekRepo;
    private final LessonRepository lessonRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final TeacherRepository teacherRepo;
    private final ManageSchemeOfWorkUseCase manageSchemeOfWorkUseCase;
    private final CreateWeekUseCase weekUseCase;
    private final CreateLessonUseCase createLessonUseCase;
    private final UpdateLessonStateUseCase updateLessonStateUseCase;
    private final UpdateWeekStateUseCase updateWeekStateUseCase;
    private final GetTeacherProgressUseCase getTeacherProgressUseCase;
    private final GetTeacherProgressDetailUseCase getTeacherProgressDetailUseCase;

    public Page<SchemeOfWorkDTO> searchScheme(int page, int size, String school, String subjectId, String sessionId, String termId, String progress) {
        Pageable pageable = PageableMapper.toPage(page, size);
        var schemes = repo.search(school, blankToNull(subjectId), blankToNull(sessionId), blankToNull(termId), parseProgress(progress), pageable);
        return mapWithProgress(schemes);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private Week.State parseProgress(String value) {
        if (value == null || value.isBlank())
            return null;

        try {
            return Week.State.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Attaches each scheme's rolled-up week progress in one bulk query, rather than
    // querying weeks per scheme (N+1) while mapping the page.
    private Page<SchemeOfWorkDTO> mapWithProgress(Page<SchemeOfWork> schemes) {
        var schemeIds = schemes.getContent().stream().map(SchemeOfWork::getId).toList();

        Map<String, Week.State> progressByScheme = weekRepo.findAllBySchemeIds(schemeIds).stream()
                .collect(Collectors.groupingBy(
                        w -> w.getScheme().getId(),
                        Collectors.mapping(Week::getState, Collectors.collectingAndThen(Collectors.toList(), Week::deriveProgress))));

        return schemes.map(s -> SchemeOfWorkMapper.toDTO(s, progressByScheme.getOrDefault(s.getId(), Week.State.NOT_STARTED)));
    }

    public SchemeOfWorkDTO getScheme(String id) {
        var domain = repo.findById(id).orElseThrow(() -> new NotFoundException("scheme not found"));
        var progress = Week.deriveProgress(weekRepo.findAllByScheme(id).stream().map(Week::getState).toList());
        return SchemeOfWorkMapper.toDTO(domain, progress);
    }

    public SchemeProgressDTO getSchemeProgress(String id) {
        var weeks = weekRepo.findAllByScheme(id);

        int totalWeeks = weeks.size();

        int completed = (int) weeks.stream()
                .filter(w -> w.getState() == Week.State.COMPLETED)
                .count();

        int remaining = totalWeeks - completed;

        BigDecimal coverage = totalWeeks == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalWeeks), 2, RoundingMode.HALF_UP);

        return new SchemeProgressDTO(
                id,
                totalWeeks,
                completed,
                remaining,
                coverage);
    }

    public SchemeOfWorkDTO create(String schoolId, String sessionId, String termId, String subjectId) {
        var res = manageSchemeOfWorkUseCase.execute(schoolId, subjectId, sessionId, termId);
        return SchemeOfWorkMapper.toDTO(res);
    }

    public WeekDTO createWeek(String schoolId, String scheme, int week, String topic, String subtopic,
            List<String> objectives) {
        var res = weekUseCase.execute(schoolId, scheme, week, topic, subtopic, objectives);
        return WeekMapper.toDTO(res);
    }

    public List<WeekDTO> getWeeksBySchema(String scheme) {
        return weekRepo.findAllByScheme(scheme).stream().map(WeekMapper::toDTO).toList();
    }

    public Page<WeekDTO> getWeeksByAcademicYear(String school, String academicYearId, int page, int size) {
        Pageable pageable = PageableMapper.toPage(page, size);
        var year = resolveAcademicYearUseCase.execute(school, academicYearId);
        return weekRepo.findBySchemeSessionAcademicYear(year.getId(), pageable).map(WeekMapper::toDTO);
    }

    public List<WeekDTO> getWeeks(String scheme) {
        return weekRepo.findAllByScheme(scheme).stream().map(WeekMapper::toDTO).toList();
    }

    public WeekDTO getWeek(String id) {
        var week = weekRepo.findById(id).orElseThrow(() -> new NotFoundException("week not found"));
        return WeekMapper.toDTO(week);
    }

    public WeekDTO updateWeekState(String schoolId, String id, String state) {
        var res = updateWeekStateUseCase.execute(schoolId, id, Week.State.valueOf(state));
        return WeekMapper.toDTO(res);
    }

    public Page<SchemeOfWorkDTO> searchMyScheme(String schoolId, String userId, int page, int size, String subjectId, String sessionId, String termId, String progress) {
        var teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("teacher not found"));
        Pageable pageable = PageableMapper.toPage(page, size);
        var schemes = repo.searchByTeacher(teacher.getId(), schoolId, blankToNull(subjectId), blankToNull(sessionId), blankToNull(termId), parseProgress(progress), pageable);
        return mapWithProgress(schemes);
    }

    public LessonDTO createLesson(
            String schoolId,
            String weekId,
            String title,
            String content,
            LocalDate date,
            String duration,
            List<String> objectives,
            String previousKnowledge,
            List<String> teachingAids,
            List<String> referenceMaterials,
            List<LessonStage> presentation,
            String evaluation,
            String assignment) {
        var res = createLessonUseCase.execute(
                schoolId,
                weekId,
                title,
                content,
                date,
                duration,
                objectives,
                previousKnowledge,
                teachingAids,
                referenceMaterials,
                presentation,
                evaluation,
                assignment);
        return LessonMapper.toDTO(res);
    }

    public List<LessonDTO> getLessons(String weekId) {
        return lessonRepo.findAllByWeek(weekId).stream().map(LessonMapper::toDTO).toList();
    }

    public LessonDTO getLesson(String id) {
        var lesson = lessonRepo.findById(id).orElseThrow(() -> new NotFoundException("lesson not found"));
        return LessonMapper.toDTO(lesson);
    }

    public LessonDTO updateLessonState(String schoolId, String id, String state) {
        var res = updateLessonStateUseCase.execute(schoolId, id, Lesson.State.valueOf(state));
        return LessonMapper.toDTO(res);
    }

    public Page<LessonDTO> searchMyLessons(String schoolId, String userId, int page, int size) {
        var teacher = teacherRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("teacher not found"));
        Pageable pageable = PageableMapper.toPage(page, size);
        return lessonRepo.findAllByTeacherIdAndSchoolId(teacher.getId(), schoolId, pageable).map(LessonMapper::toDTO);
    }

    public List<TeacherProgressDTO> getTeacherProgress(String schoolId) {
        return getTeacherProgressUseCase.execute(schoolId);
    }

    public TeacherProgressDetailDTO getTeacherProgress(String schoolId, String teacherId) {
        return getTeacherProgressDetailUseCase.execute(schoolId, teacherId);
    }
}
