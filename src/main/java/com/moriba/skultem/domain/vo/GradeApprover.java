package com.moriba.skultem.domain.vo;

// Who approves a subject teacher's submitted grades. CLASS_MASTER: the class's class master (an admin,
// proprietor or owner can always step in, and a class master who taught the subject themselves hands
// it to an admin). ADMIN: only an admin, proprietor or owner - the class master isn't involved. Set per
// school, and a management section can override it (see GradeApprovalResolver).
public enum GradeApprover {
    CLASS_MASTER,
    ADMIN
}
