package com.moriba.skultem.application.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import com.moriba.skultem.domain.model.User;

@RequiredArgsConstructor
@Getter
public class SupplyCollectedEvent {
    private final String schoolId;
    private final User user;
    private final String studentName;
    private final String materialName;
    private final int qty;
    private final boolean fullyCollected;
    private final Map<String, String> meta;
}
