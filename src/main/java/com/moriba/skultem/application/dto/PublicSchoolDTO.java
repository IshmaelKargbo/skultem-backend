package com.moriba.skultem.application.dto;

import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.vo.Address;

// What the unauthenticated, public school directory (marketing site) may see - deliberately just
// the school's public identity and general location, never the owner, status, settings or street.
public record PublicSchoolDTO(String name, String domain, String url, String logo, String motto, String city,
        String district, String region, String primaryColor) {

    public static PublicSchoolDTO from(School school) {
        Address a = school.getAddress();
        return new PublicSchoolDTO(school.getName(), school.getDomain(),
                "https://" + school.getDomain() + ".skultem.space", school.getLogo(), school.getMotto(),
                a != null ? a.city() : null, a != null ? a.district() : null, a != null ? a.region() : null,
                school.getPrimaryColor());
    }
}
