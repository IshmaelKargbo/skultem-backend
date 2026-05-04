package com.moriba.skultem.application.usecase;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.UserDTO;
import com.moriba.skultem.application.error.AccessDeniedException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.UserMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ResetPasswordUseCase {
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepo;
    private final PasswordEncoder passwordEncoder;

    @AuditLogAnnotation(action = "RESET_PASSWORD")
    public UserDTO execute(ResetPassword param) {
        var user = userRepository.findById(param.userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var password = passwordEncoder.encode(param.password);
        user.resetPassword(password);
        userRepository.save(user);
        List<Role> roles = schoolUserRepo.findAllByUser_IdAndSchoolId(param.userId, param.schoolId()).stream().map(e -> e.getRole())
                .toList();
        return UserMapper.toDTO(user, roles);
    }

    public record ResetPassword(String userId, String password, String schoolId) {
    }
}
