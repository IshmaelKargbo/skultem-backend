package com.moriba.skultem.domain.vo;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ReferenceSequenceId implements Serializable {
    private String referenceType;
    private Integer year;
}
