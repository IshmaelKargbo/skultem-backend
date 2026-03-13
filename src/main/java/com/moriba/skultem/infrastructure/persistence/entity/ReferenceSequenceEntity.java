package com.moriba.skultem.infrastructure.persistence.entity;

import com.moriba.skultem.domain.vo.ReferenceSequenceId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "reference_sequences")
@IdClass(ReferenceSequenceId.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceSequenceEntity {
    
    @Id
    private String referenceType;

    @Id
    private Integer year;

    @Column(nullable = false)
    private Integer lastNumber;
}
