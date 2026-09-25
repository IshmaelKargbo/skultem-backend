package com.moriba.skultem.application.dto;

import com.moriba.skultem.domain.vo.Address;

// The branding to print for one level / student / staff member, already resolved (the section's
// own, else the school's). logo/principalSignature are inline data: URIs when asked for (PDF
// capture) and the plain URLs otherwise. principalName/address ride along so a document can show
// the right head and location; ownPrincipal/ownAddress say those came from the section itself.
public record SchoolBrandingAssetsDTO(String logo, String principalSignature, String principalName,
        Address address, boolean ownPrincipal, boolean ownAddress) {
}
