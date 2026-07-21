package com.moriba.skultem.application.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchemeOfWorkDTO;
import com.moriba.skultem.application.dto.SchemeProgressDTO;
import com.moriba.skultem.application.dto.WeekDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.PageableMapper;
import com.moriba.skultem.application.mapper.SchemeOfWorkMapper;
import com.moriba.skultem.application.mapper.WeekMapper;
import com.moriba.skultem.application.usecase.CreateWeekUseCase;
import com.moriba.skultem.application.usecase.ManageSchemeOfWorkUseCase;
import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.WeekRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final SchemeOfWorkRepository repo;
    private final WeekRepository weekRepo;
    private final AcademicYearRepository yearRepo;
    private final ManageSchemeOfWorkUseCase manageSchemeOfWorkUseCase;
    private final CreateWeekUseCase weekUseCase;

    public Page<SchemeOfWorkDTO> searchScheme(int page, int size, String school) {
        Pageable pageable = PageableMapper.toPage(page, size);
        return repo.findAllBySchoolId(school, pageable).map(SchemeOfWorkMapper::toDTO);
    }

    public SchemeOfWorkDTO getScheme(String id) {
        var domain = repo.findById(id).orElseThrow(() -> new NotFoundException("scheme not found"));
        return SchemeOfWorkMapper.toDTO(domain);
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

    public Page<WeekDTO> getWeeksByAcademicYear(String school, int page, int size) {
        Pageable pageable = PageableMapper.toPage(page, size);
        var year = yearRepo.findActiveBySchool(school)
                .orElseThrow(() -> new NotFoundException("no active academic year found"));
        return weekRepo.findBySchemeSessionAcademicYear(year.getId(), pageable).map(WeekMapper::toDTO);
    }

    public List<WeekDTO> getWeeks(String scheme) {
        return weekRepo.findAllByScheme(scheme).stream().map(WeekMapper::toDTO).toList();
    }
}
