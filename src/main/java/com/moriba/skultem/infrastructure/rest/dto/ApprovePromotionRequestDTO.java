package com.moriba.skultem.infrastructure.rest.dto;

import java.util.List;

/**
 * @param allowToPassEnrollmentIds enrollments the admin is overriding from REPEAT to PROMOTE as
 *                                 part of this approval, in addition to whatever the class master
 *                                 submitted.
 */
public record ApprovePromotionRequestDTO(String note, List<String> allowToPassEnrollmentIds) {
}
