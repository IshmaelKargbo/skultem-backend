package com.moriba.skultem.application.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.domain.model.AttendanceLocationSetting;
import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.Role;

import lombok.RequiredArgsConstructor;

// Decides whether a clock-in / clock-out is at an allowed place. A school run from several places
// has a location per management section (plus optionally a school-wide one), so which locations
// count depends on who's clocking in:
//   * limited to some sections -> those sections' locations, or the school-wide one if none of them
//     has its own;
//   * whole-school staff (not limited to any section) -> any configured location, since they can
//     work at any site.
// Being in range of ANY allowed location is enough. Each location's own IP allowlist still applies
// to that location. Enforced server-side - never trusted from the client.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClockInLocationService {

    public enum Action {
        CLOCK_IN("Clock-in", "clock in"),
        CLOCK_OUT("Clock-out", "clock out");

        final String noun;
        final String verb;

        Action(String noun, String verb) {
            this.noun = noun;
            this.verb = verb;
        }
    }

    public record Match(AttendanceLocationSetting setting, double distanceMeters) {
    }

    private final AttendanceLocationSettingRepository locationRepo;
    private final StaffManagementSectionRepository staffSectionRepo;

    public Match check(String schoolId, Teacher teacher, double latitude, double longitude, Double accuracyMeters,
            String ip, Action action) {
        var candidates = candidatesFor(schoolId, teacher);
        if (candidates.isEmpty()) {
            throw new BadRequestException("Clock-in location has not been set up yet - contact your school admin");
        }

        var ipAllowed = candidates.stream()
                .filter(c -> !c.hasIpRestriction() || c.isIpAllowed(ip))
                .toList();
        if (ipAllowed.isEmpty()) {
            throw new BadRequestException(action.noun + " must be done from the school's network.");
        }

        var inRange = ipAllowed.stream()
                .filter(c -> c.isWithinRange(latitude, longitude, accuracyMeters))
                .min(Comparator.comparingDouble(c -> c.distanceMetersTo(latitude, longitude)));
        if (inRange.isPresent()) {
            return new Match(inRange.get(), inRange.get().distanceMetersTo(latitude, longitude));
        }

        var nearest = ipAllowed.stream()
                .min(Comparator.comparingDouble(c -> c.distanceMetersTo(latitude, longitude)))
                .orElseThrow();
        throw new BadRequestException(String.format(
                "You're about %.0fm from %s - you need to be within %dm to %s.",
                nearest.distanceMetersTo(latitude, longitude),
                candidates.size() > 1 ? "the nearest school location" : "the school",
                nearest.getRadiusMeters(), action.verb));
    }

    List<AttendanceLocationSetting> candidatesFor(String schoolId, Teacher teacher) {
        var all = locationRepo.findAllBySchoolId(schoolId);
        var sectionIds = staffSectionRepo
                .findBySchoolAndUserAndRole(schoolId, teacher.getUser().getId(), Role.TEACHER).stream()
                .map(StaffManagementSection::getManagementSectionId)
                .toList();

        if (sectionIds.isEmpty()) {
            return all;
        }

        var own = all.stream()
                .filter(l -> l.getManagementSectionId() != null && sectionIds.contains(l.getManagementSectionId()))
                .toList();
        if (!own.isEmpty()) {
            return own;
        }
        return all.stream().filter(l -> l.getManagementSectionId() == null).toList();
    }
}
