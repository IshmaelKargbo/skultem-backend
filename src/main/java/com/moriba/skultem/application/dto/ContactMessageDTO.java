package com.moriba.skultem.application.dto;

import java.time.Instant;

public record ContactMessageDTO(String id, String name, String email, String school, String phone, String challenge,
        String status, Instant createdAt, Instant updatedAt, LeadDTO lead) {

    // lead is the CRM record this message was made into - null until one exists.
    public ContactMessageDTO withLead(LeadDTO lead) {
        return new ContactMessageDTO(id, name, email, school, phone, challenge, status, createdAt, updatedAt, lead);
    }
}
