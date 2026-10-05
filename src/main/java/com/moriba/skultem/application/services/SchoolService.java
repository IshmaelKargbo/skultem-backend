package com.moriba.skultem.application.services;

import java.util.List;

import com.moriba.skultem.application.dto.PublicSchoolDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.application.usecase.BackfillPlatformFeesUseCase;
import com.moriba.skultem.domain.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository repo;
    private final BackfillPlatformFeesUseCase backfillPlatformFeesUseCase;

    public SchoolDTO get(String id) {
       var domain = repo.findById(id).orElseThrow(() -> new NotFoundException("school not found"));

       // Fire-and-forget: reconciles the platform fee setting/structure/student assignments for
       // this school in the background (throttled - see BackfillPlatformFeesUseCase) every time
       // its info is fetched, not just at the last app restart. Never delays this response.
       backfillPlatformFeesUseCase.executeForSchoolAsync(domain.getId());

       return SchoolMapper.toDTO(domain);
    }

    // Live schools only (ACTIVE, not Playground) - the public figure shown on the login page.
    public long countLive() {
        return repo.countLive();
    }

    public List<PublicSchoolDTO> listLive() {
        return repo.findAllLive().stream().map(PublicSchoolDTO::from).toList();
    }

}
