package com.moriba.skultem.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.model.Student.EnrollmentType;

class FeeStudentGroupTest {

    private FeeStructure fee(boolean newOnly, boolean oldOnly) {
        return FeeStructure.create("s", FeeStructure.Type.ALL, null, false, List.of(), null, null, null, null,
                new BigDecimal("100"), "Admission", false, newOnly, oldOnly, null, null);
    }

    @Test
    void everyStudentQualifiesForExactlyOneOfTheNewAndOldStudentFees() {
        FeeStructure newFee = fee(true, false);
        FeeStructure oldFee = fee(false, true);
        for (EnrollmentType type : EnrollmentType.values()) {
            assertThat(newFee.appliesToEnrollmentType(type) ^ oldFee.appliesToEnrollmentType(type))
                    .as(type.name()).isTrue();
        }
        assertThat(newFee.appliesToEnrollmentType(EnrollmentType.TRANSFER)).isTrue();
        assertThat(oldFee.appliesToEnrollmentType(EnrollmentType.EXISTING)).isTrue();
        assertThat(oldFee.appliesToEnrollmentType(EnrollmentType.RE_ENROLLMENT)).isTrue();
    }

    @Test
    void aFeeForEveryoneAppliesToEveryType() {
        for (EnrollmentType type : EnrollmentType.values()) {
            assertThat(fee(false, false).appliesToEnrollmentType(type)).isTrue();
        }
    }
}
