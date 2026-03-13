package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportConfigDTO;
import com.moriba.skultem.application.mapper.ReportConfigViewMapper;
import com.moriba.skultem.domain.model.ReportConfig;
import com.moriba.skultem.domain.repository.ReportConfigRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateReportConfigUseCase {
    private final ReportConfigRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public ReportConfigDTO execute(CreateReportConfigRecord param) {
        String id = rg.generate("REPORT_CONFIG", "RPC");
        ReportConfig config = ReportConfig.create(
                id,
                param.schoolId(),
                param.name(),
                param.type(),
                param.format(),
                param.classId(),
                param.classSessionId(),
                param.teacherSubjectId(),
                param.termId(),
                param.startDate(),
                param.endDate());

        repo.save(config);
        return ReportConfigViewMapper.toDTO(config);
    }

    public record CreateReportConfigRecord(
            String schoolId,
            String name,
            String type,
            String format,
            String classId,
            String classSessionId,
            String teacherSubjectId,
            String termId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {
    }
}
