package com.moriba.skultem.domain.vo;

public enum Role {
    OWNER,
    SYSTEM_ADMIN,
    PROPRIETOR,
    PRINCIPAL,
    SUPER_ADMIN,
    ADMIN,
    ACCOUNTANT,
    // Runs the school library (see LibraryController) and nothing else.
    LIBRARIAN,
    TEACHER,
    PARENT
}
