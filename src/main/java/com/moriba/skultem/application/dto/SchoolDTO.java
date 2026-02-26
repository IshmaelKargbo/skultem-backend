package com.moriba.skultem.application.dto;

import java.time.Instant;

import com.moriba.skultem.domain.model.School.Status;
import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.domain.model.vo.Owner;

public record SchoolDTO(String id, String name, String domain, Address address, Owner owner, Status status, Instant createdAt, Instant updatedAt) {
    
}
