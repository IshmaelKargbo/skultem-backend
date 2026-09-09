package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;

// Covers the "school wants to change its clock-in location" flow end to end through the real
// use case + repository - the same upsert-by-schoolId path the Attendance settings page's Save
// button hits (AttendanceLocationSettingController PUT /api/v1/attendance-location).
@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class SaveAttendanceLocationSettingUseCaseTest {

    @DynamicPropertySource
    static void h2Props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:skultem3;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR");
    }

    @Autowired
    private SaveAttendanceLocationSettingUseCase useCase;

    @Autowired
    private GetAttendanceLocationSettingUseCase getUseCase;

    @Autowired
    private AttendanceLocationSettingRepository repo;

    @Test
    @Transactional
    void createsASettingWhenTheSchoolHasNoneYet() {
        String schoolId = "school-location-1";

        assertThat(getUseCase.execute(schoolId).configured()).isFalse();

        var result = useCase.execute(schoolId, 8.4672512, -13.2317184, 150, null);

        assertThat(result.configured()).isTrue();
        assertThat(result.latitude()).isEqualTo(8.4672512);
        assertThat(result.longitude()).isEqualTo(-13.2317184);
        assertThat(result.radiusMeters()).isEqualTo(150);

        var persisted = repo.findBySchoolId(schoolId).orElseThrow();
        assertThat(persisted.getLatitude()).isEqualTo(8.4672512);
        assertThat(persisted.getRadiusMeters()).isEqualTo(150);
    }

    @Test
    @Transactional
    void movingTheLocationUpdatesTheExistingRowInPlaceRatherThanCreatingASecondOne() {
        String schoolId = "school-location-2";

        useCase.execute(schoolId, 8.4672512, -13.2317184, 150, null);
        var originalId = repo.findBySchoolId(schoolId).orElseThrow().getId();

        // The school relocates - same flow as pressing "Save Location" again with new
        // coordinates and a wider radius on the settings page.
        var updated = useCase.execute(schoolId, 8.4841, -13.2354, 250, "41.66.12.5");

        assertThat(updated.latitude()).isEqualTo(8.4841);
        assertThat(updated.longitude()).isEqualTo(-13.2354);
        assertThat(updated.radiusMeters()).isEqualTo(250);
        assertThat(updated.allowedIps()).isEqualTo("41.66.12.5");

        var persisted = repo.findBySchoolId(schoolId).orElseThrow();
        assertThat(persisted.getId()).isEqualTo(originalId);
        assertThat(persisted.getLatitude()).isEqualTo(8.4841);
        assertThat(persisted.getAllowedIps()).isEqualTo("41.66.12.5");

        // Fetching it back (what the settings page does on load) reflects the move too.
        var fetched = getUseCase.execute(schoolId);
        assertThat(fetched.latitude()).isEqualTo(8.4841);
        assertThat(fetched.radiusMeters()).isEqualTo(250);
    }

    @Test
    @Transactional
    void twoSchoolsKeepIndependentLocations() {
        useCase.execute("school-location-3a", 8.4672512, -13.2317184, 150, null);
        useCase.execute("school-location-3b", 9.0, -14.0, 100, null);

        assertThat(getUseCase.execute("school-location-3a").latitude()).isEqualTo(8.4672512);
        assertThat(getUseCase.execute("school-location-3b").latitude()).isEqualTo(9.0);
    }
}
