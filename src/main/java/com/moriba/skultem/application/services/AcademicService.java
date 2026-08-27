package com.moriba.skultem.application.services;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.application.usecase.ResolveAcademicYearUseCase;
import com.moriba.skultem.domain.repository.TermRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcademicService {

    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final TermRepository termRepo;

    public List<TermDTO> getAcademicTerms(String schoolId, String academicYearId) {
       var year = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
       var terms = termRepo.findByAcademicYearIdAndSchool(year.getId(), schoolId);
       return terms.stream().map(TermMapper::toDTO).toList();
    }

}
