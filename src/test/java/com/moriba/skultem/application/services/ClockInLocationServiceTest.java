package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.services.ClockInLocationService.Action;
import com.moriba.skultem.domain.model.AttendanceLocationSetting;
import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.Role;

// A school with a campus in Freetown (Primary section) and one in Bo (Secondary section).
@ExtendWith(MockitoExtension.class)
class ClockInLocationServiceTest {

    private static final String SCHOOL = "school-1";
    private static final double FREETOWN_LAT = 8.4840, FREETOWN_LNG = -13.2299;
    private static final double BO_LAT = 7.9647, BO_LNG = -11.7383;

    @Mock
    private AttendanceLocationSettingRepository locationRepo;
    @Mock
    private StaffManagementSectionRepository staffSectionRepo;
    @InjectMocks
    private ClockInLocationService service;

    private final Teacher teacher = mock(Teacher.class, RETURNS_DEEP_STUBS);

    private AttendanceLocationSetting primary;
    private AttendanceLocationSetting secondary;

    @BeforeEach
    void setUp() {
        when(teacher.getUser().getId()).thenReturn("user-1");
        primary = AttendanceLocationSetting.createForSection("a", SCHOOL, "sec-primary", FREETOWN_LAT, FREETOWN_LNG,
                150, null);
        secondary = AttendanceLocationSetting.createForSection("b", SCHOOL, "sec-secondary", BO_LAT, BO_LNG, 150,
                null);
        when(locationRepo.findAllBySchoolId(SCHOOL)).thenReturn(List.of(primary, secondary));
    }

    private void limitedTo(String... sectionIds) {
        when(staffSectionRepo.findBySchoolAndUserAndRole(SCHOOL, "user-1", Role.TEACHER)).thenReturn(
                java.util.Arrays.stream(sectionIds)
                        .map(id -> StaffManagementSection.create(SCHOOL, "user-1", Role.TEACHER, id)).toList());
    }

    @Test
    void aTeacherLimitedToASectionMustBeAtThatSectionsCampus() {
        limitedTo("sec-secondary");

        var match = service.check(SCHOOL, teacher, BO_LAT, BO_LNG, null, "1.1.1.1", Action.CLOCK_IN);
        assertThat(match.setting().getManagementSectionId()).isEqualTo("sec-secondary");

        // Standing at the OTHER section's campus doesn't count for them.
        assertThatThrownBy(() -> service.check(SCHOOL, teacher, FREETOWN_LAT, FREETOWN_LNG, null, "1.1.1.1",
                Action.CLOCK_IN))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("within 150m to clock in");
    }

    @Test
    void wholeSchoolStaffMayClockInAtAnyCampus() {
        limitedTo();

        assertThat(service.check(SCHOOL, teacher, FREETOWN_LAT, FREETOWN_LNG, null, "1.1.1.1", Action.CLOCK_IN)
                .setting().getManagementSectionId()).isEqualTo("sec-primary");
        assertThat(service.check(SCHOOL, teacher, BO_LAT, BO_LNG, null, "1.1.1.1", Action.CLOCK_OUT)
                .setting().getManagementSectionId()).isEqualTo("sec-secondary");
    }

    @Test
    void aSectionWithNoLocationOfItsOwnFallsBackToTheSchoolWideOne() {
        var schoolWide = AttendanceLocationSetting.create("c", SCHOOL, 9.0, -12.0, 150, null);
        when(locationRepo.findAllBySchoolId(SCHOOL)).thenReturn(List.of(primary, schoolWide));
        limitedTo("sec-secondary");

        assertThat(service.check(SCHOOL, teacher, 9.0, -12.0, null, "1.1.1.1", Action.CLOCK_IN).setting().getId())
                .isEqualTo("c");
    }

    @Test
    void eachLocationsOwnIpAllowlistApplies() {
        secondary.update(BO_LAT, BO_LNG, 150, "41.66.12.5");
        limitedTo("sec-secondary");

        assertThatThrownBy(() -> service.check(SCHOOL, teacher, BO_LAT, BO_LNG, null, "9.9.9.9", Action.CLOCK_OUT))
                .isInstanceOf(BadRequestException.class).hasMessage("Clock-out must be done from the school's network.");
        assertThat(service.check(SCHOOL, teacher, BO_LAT, BO_LNG, null, "41.66.12.5", Action.CLOCK_OUT)).isNotNull();
    }

    @Test
    void nothingConfiguredIsReported() {
        when(locationRepo.findAllBySchoolId(SCHOOL)).thenReturn(List.of());
        limitedTo();

        assertThatThrownBy(() -> service.check(SCHOOL, teacher, 0, 0, null, "1.1.1.1", Action.CLOCK_IN))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("has not been set up");
    }
}
