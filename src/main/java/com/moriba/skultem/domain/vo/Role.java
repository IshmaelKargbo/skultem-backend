package com.moriba.skultem.domain.vo;

public enum Role {
    OWNER,
    SYSTEM_ADMIN,
    PROPRIETOR,
    // Staff member with access to the whole school portal (see PermissionService#SUPER_ADMIN_COVERS).
    SUPER_ADMIN,
    ADMIN,
    ACCOUNTANT,
    TEACHER,
    PARENT
}
