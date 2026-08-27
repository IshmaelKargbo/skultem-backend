package com.moriba.skultem.infrastructure.persistence.jpa;


import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.RequestDemoEntity;

public interface RequestDemoJpaRepository extends JpaRepository<RequestDemoEntity, String> {}