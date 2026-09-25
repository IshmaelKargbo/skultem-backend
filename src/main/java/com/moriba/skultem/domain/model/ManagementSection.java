package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Address;

import lombok.Getter;

// A school-defined management grouping of one or more of its levels - e.g. "Early Years & Primary"
// managing Daycare + Nursery + Primary. Only exists in a SECTION_BASED school; which levels it
// manages is recorded on SchoolLevel. Unrelated to Section (a class division like "A") and Stream.
// It can carry its own logo / principal / signature / address for a school run from different
// places; anything left null falls back to the school's own (see SchoolBrandingResolver).
@Getter
public class ManagementSection extends AggregateRoot<String> {

    private String schoolId;
    private String name;
    private int displayOrder;
    private String logo;
    private String principalName;
    private String principalSignature;
    private Address address;

    public ManagementSection(String id, String schoolId, String name, int displayOrder, String logo,
            String principalName, String principalSignature, Address address, Instant createdAt,
            Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.name = name;
        this.displayOrder = displayOrder;
        this.logo = logo;
        this.principalName = principalName;
        this.principalSignature = principalSignature;
        this.address = address;
        touch(updatedAt);
    }

    public static ManagementSection create(String schoolId, String name, int displayOrder) {
        Instant now = Instant.now();
        return new ManagementSection(UUID.randomUUID().toString(), schoolId, name, displayOrder, null, null, null, null, now,
                now);
    }

    public void update(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
        touch(Instant.now());
    }

    public void updateBranding(String logo, String principalName, String principalSignature, Address address) {
        this.logo = logo;
        this.principalName = principalName;
        this.principalSignature = principalSignature;
        this.address = address;
        touch(Instant.now());
    }
}
