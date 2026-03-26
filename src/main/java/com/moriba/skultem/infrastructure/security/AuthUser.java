package com.moriba.skultem.infrastructure.security;

import com.moriba.skultem.domain.vo.Role;

public record AuthUser(
        String userId,
        String activeSchoolId,
        Role activeRole
) {}

