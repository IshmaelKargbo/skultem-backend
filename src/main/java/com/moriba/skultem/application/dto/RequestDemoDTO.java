package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.UUID;

// lead is the CRM record this request was made into (status, owner, demo date) - null until one exists.
public record RequestDemoDTO(UUID id, String name, String email, String school, String phone, String city,
        String address, String preferred, String priority, String message, Instant createdAt, Instant updatedAt,
        LeadDTO lead) {

    public RequestDemoDTO(UUID id, String name, String email, String school, String phone, String city,
            String address, String preferred, String priority, String message, Instant createdAt, Instant updatedAt) {
        this(id, name, email, school, phone, city, address, preferred, priority, message, createdAt, updatedAt, null);
    }

    public RequestDemoDTO withLead(LeadDTO lead) {
        return new RequestDemoDTO(id, name, email, school, phone, city, address, preferred, priority, message,
                createdAt, updatedAt, lead);
    }
}
