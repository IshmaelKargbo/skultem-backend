package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NationalCalendarDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.NationalCalendarMapper;
import com.moriba.skultem.domain.model.NationalAcademicYear;
import com.moriba.skultem.domain.model.NationalAcademicYear.NationalTerm;
import com.moriba.skultem.domain.repository.NationalCalendarRepository;
import com.moriba.skultem.infrastructure.rest.dto.SaveNationalCalendarDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Creates or updates a platform-wide academic year and makes it the current one. A year is
 * identified by name: saving an existing name edits it in place, a new name adds a new year (and
 * the previous current year simply stops being current - it's kept, not deleted). Terms are
 * numbered by their order and must fall inside the year, in order, without overlapping.
 * Already-created schools are never touched - see {@link NationalAcademicYear}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SaveNationalCalendarUseCase {

    private final NationalCalendarRepository repo;

    public NationalCalendarDTO execute(SaveNationalCalendarDTO param) {
        var name = param.name().trim();
        var terms = validate(param, name);

        var year = repo.findByName(name).orElse(null);
        if (year == null) {
            year = NationalAcademicYear.create(UUID.randomUUID().toString(), name, param.startDate(),
                    param.endDate(), terms);
        } else {
            year.update(param.startDate(), param.endDate(), terms);
        }

        repo.save(year);
        return NationalCalendarMapper.toDTO(year);
    }

    private List<NationalTerm> validate(SaveNationalCalendarDTO param, String name) {
        if (name.isEmpty()) {
            throw new RuleException("Academic year name is required");
        }
        if (!param.startDate().isBefore(param.endDate())) {
            throw new RuleException("Academic year start date must be before its end date");
        }

        var terms = new ArrayList<NationalTerm>();
        LocalDate previousEnd = null;
        int number = 1;

        for (var t : param.terms()) {
            if (!t.startDate().isBefore(t.endDate())) {
                throw new RuleException("Term " + number + " start date must be before its end date");
            }
            if (t.startDate().isBefore(param.startDate()) || t.endDate().isAfter(param.endDate())) {
                throw new RuleException("Term " + number + " must fall within the academic year");
            }
            if (previousEnd != null && !t.startDate().isAfter(previousEnd)) {
                throw new RuleException("Term " + number + " must start after Term " + (number - 1) + " ends");
            }

            terms.add(new NationalTerm(number, t.name().trim(), t.startDate(), t.endDate()));
            previousEnd = t.endDate();
            number++;
        }

        return terms;
    }
}
