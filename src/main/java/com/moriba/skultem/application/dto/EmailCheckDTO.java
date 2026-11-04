package com.moriba.skultem.application.dto;

// The result of checking an address before it's saved. `status`:
//   OK              - well-formed and its domain accepts mail (that doesn't prove the mailbox exists - only
//                     verification does);
//   INVALID_FORMAT  - not an email address;
//   NO_SUCH_DOMAIN  - the domain doesn't exist (often a typo, e.g. gmial.com);
//   NO_MAIL_SERVER  - the domain exists but can't receive email;
//   UNCHECKED       - the domain couldn't be checked right now (DNS slow or down) - not treated as invalid.
public record EmailCheckDTO(String email, String status, boolean deliverable, String message) {
}
