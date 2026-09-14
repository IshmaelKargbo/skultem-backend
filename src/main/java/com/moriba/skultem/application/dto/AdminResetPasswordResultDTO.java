package com.moriba.skultem.application.dto;

// Returned only to the admin who triggered the reset, so they can copy temporaryPassword and
// share it with the staff member (call, chat, in person) - it's never emailed or logged, and
// the plaintext is cleared from the account (see User.resetPassword) as soon as they log in
// with it and set their own password.
public record AdminResetPasswordResultDTO(UserDTO user, String temporaryPassword) {
}
