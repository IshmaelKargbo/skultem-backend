package com.moriba.skultem.application.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ResetPasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @AuditLogAnnotation(action = "RESET_PASSWORD")
    public UserDTO execute(ResetPassword param) {
        var user = userRepository.findById(param.userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var password = passwordEncoder.encode(param.password);
        user.resetPassword(password);
        userRepository.save(user);

        return UserMapper.toDTO(user);
    }

    public record ResetPassword(String userId, String password, String schoolId) {
    }
}
