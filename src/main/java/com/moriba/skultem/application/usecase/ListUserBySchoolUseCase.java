package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.model.Role;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListUserBySchoolUseCase {

    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;

    public Page<UserDTO> execute(String school, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        return repo.findBySchool(school, pageable)
                .map(user -> {
                    List<Role> roles = schoolUserRepo.findAllByUser_IdAndSchoolId(user.getId(), school).stream()
                            .map(e -> e.getRole())
                            .toList();
                    return UserMapper.toDTO(user, roles);
                });
    }
}
