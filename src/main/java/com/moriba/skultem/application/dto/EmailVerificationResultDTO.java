package com.moriba.skultem.application.dto;

// What opening a verification link did. status:
//   VERIFIED         - the address is now verified;
//   ALREADY_VERIFIED - it already was (the link had been opened before);
//   EXPIRED          - the link is past its expiry - ask for a new one;
//   SUPERSEDED       - a newer link was sent, so this one no longer works;
//   EMAIL_CHANGED    - the account's email changed after this link was sent;
//   INVALID          - not a link we sent.
// email is masked (j***@example.com) - the page is public.
public record EmailVerificationResultDTO(String status, String email) {
}
