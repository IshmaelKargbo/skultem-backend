package com.moriba.skultem.infrastructure.persistence.jpa;


import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.RequestDemoEntity;

public interface RequestDemoJpaRepository extends JpaRepository<RequestDemoEntity, UUID> {}