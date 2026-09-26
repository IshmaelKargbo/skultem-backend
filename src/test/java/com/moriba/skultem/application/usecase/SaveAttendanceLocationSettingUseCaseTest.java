package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AttendanceLocationSettingMapper;
import com.moriba.skultem.domain.model.AttendanceLocationSetting;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

// An untouched form holds 0,0 - saving that as a school's clock-in location would make every clock-in fail.
@ExtendWith(MockitoExtension.class)
class SaveAttendanceLocationSettingUseCaseTest {

    @Mock
    private AttendanceLocationSettingRepository repo;
    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private ManagementSectionRepository sectionRepo;
    @InjectMocks
    private SaveAttendanceLocationSettingUseCase useCase;

    @Test
    void zeroZeroIsRefusedForTheSchoolAndForASection() {
        assertThatThrownBy(() -> useCase.execute("school", 0, 0, 150, null)).isInstanceOf(RuleException.class)
                .hasMessageContaining("Pick the location on the map");
        assertThatThrownBy(() -> useCase.executeForSection("school", "sec", 0, 0, 150, null))
                .isInstanceOf(RuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void aRealLocationIsSaved() {
        var res = useCase.execute("school", 8.4693, -13.2381, 150, null);

        assertThat(res.configured()).isTrue();
        verify(repo).save(any());
    }

    @Test
    void aStoredZeroZeroReadsAsNotSetUp() {
        var stored = AttendanceLocationSetting.create("id", "school", 0, 0, 150, null);

        assertThat(AttendanceLocationSettingMapper.toDTO(stored).configured()).isFalse();
    }
}
