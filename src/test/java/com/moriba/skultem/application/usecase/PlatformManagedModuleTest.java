package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.ModuleAccessService;
import com.moriba.skultem.domain.repository.SchoolModuleRepository;

// Skully is given to schools by the Skultem team; a school can't install or remove it itself.
class PlatformManagedModuleTest {

    @Test
    void aSchoolCannotInstallSkullyItself() {
        var repo = mock(SchoolModuleRepository.class);
        var useCase = new InstallSchoolModuleUseCase(repo, mock(ModuleAccessService.class),
                mock(ListSchoolModulesUseCase.class), mock(LogActivityUseCase.class));

        assertThatThrownBy(() -> useCase.execute("S1", "whatsapp-assistant", "U1"))
                .isInstanceOf(RuleException.class).hasMessageContaining("Skultem team");
        verifyNoInteractions(repo);
    }
}
