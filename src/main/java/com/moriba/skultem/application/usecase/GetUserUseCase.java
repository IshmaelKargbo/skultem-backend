package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetUserUseCase {

    private final UserRepository repo;

    public UserDTO execute(String id) {
        var record = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserMapper.toDTO(record);
    }
}
