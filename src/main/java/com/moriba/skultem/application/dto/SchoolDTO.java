package com.moriba.skultem.application.dto;

import java.time.Instant;
import java.util.List;

import com.moriba.skultem.domain.model.School.GenderComposition;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.School.Status;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.GradeBand;
import com.moriba.skultem.domain.vo.Owner;

public record SchoolDTO(String id, String name, String domain, Address address, Owner owner, Status status,
        List<GradeBand> gradingScale, String logo, String motto, String principalName, String principalSignature,
        String primaryColor, String secondaryColor, Double attendanceThreshold, GenderComposition genderComposition,
        boolean testSchool, ManagementModel managementModel, Instant createdAt, Instant updatedAt) {

}
