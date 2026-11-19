package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.MobileDevice;

public interface MobileDeviceRepository {
    void save(MobileDevice device);

    Optional<MobileDevice> findByToken(String token);

    List<MobileDevice> findAllByUserId(String userId);

    void deleteByToken(String token);
}
