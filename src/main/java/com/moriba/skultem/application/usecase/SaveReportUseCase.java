package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SaveReportDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SaveReportMapper;
import com.moriba.skultem.domain.model.SaveReport;
import com.moriba.skultem.domain.repository.SaveReportRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SaveReportUseCase {
    private final SaveReportRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public SaveReportDTO execute(String schoolId, SaveReportRecord param) {

        if (param.id() != null) {
            var domain = repo.findByIdAndSchoolId(param.id, schoolId)
                    .orElseThrow(() -> new NotFoundException("Report not found"));
            domain.updateFilter(param.name(), param.filters());
            repo.save(domain);
            return SaveReportMapper.toDTO(domain);
        } else {
            String id = rg.generate("SAVE_REPORT", "SRP");

            if (repo.existsByName(param.name(), schoolId))
                throw new AlreadyExistsException("Report already exist with this name");

            SaveReport domain = SaveReport.create(
                    id,
                    schoolId,
                    param.name(),
                    param.filters(),
                    param.entity());

            repo.save(domain);
            return SaveReportMapper.toDTO(domain);
        }
    }

    public record SaveReportRecord(
            String id,
            String name,
            String entity,
            List<Filter> filters) {
    }
}
