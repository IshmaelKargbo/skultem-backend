package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.Role;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetUserUseCase {

    private final UserRepository repo;
    private final SchoolUserRepository schoolUserRepo;

    public UserDTO execute(String schoolId, String id) {
        var record = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        List<Role> roles = schoolUserRepo.findAllByUser_IdAndSchoolId(id, schoolId).stream().map(e -> e.getRole())
                .toList();
        return UserMapper.toDTO(record, roles);
    }
}
