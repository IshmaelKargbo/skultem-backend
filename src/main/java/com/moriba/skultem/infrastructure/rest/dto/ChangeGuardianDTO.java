package com.moriba.skultem.infrastructure.rest.dto;

import jakarta.validation.constraints.Size;

// Who the student moves to - exactly one of:
//   parentId: an existing guardian at this school, or
//   userId:   any account already at this school (a teacher who is also a parent, say). Their guardian
//             profile here is reused, or created from phone/street/city if they don't have one yet.
// relationship: how the new guardian is related to the student ("Mother", "Uncle", ...); blank keeps
// the previous one.
public record ChangeGuardianDTO(
        String parentId,
        String userId,
        @Size(max = 30, message = "Phone number is too long") String phone,
        @Size(max = 255, message = "Address is too long") String street,
        @Size(max = 100, message = "City is too long") String city,
        @Size(max = 100, message = "Relationship is too long") String relationship) {
}
