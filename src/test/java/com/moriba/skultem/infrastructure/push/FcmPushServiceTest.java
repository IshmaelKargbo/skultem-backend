package com.moriba.skultem.infrastructure.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.domain.repository.MobileDeviceRepository;

class FcmPushServiceTest {

    private static String serviceAccount() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        String key = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(gen.generateKeyPair().getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + key + "\n-----END PRIVATE KEY-----\n";
        return "{\"type\":\"service_account\",\"project_id\":\"skultem-test\","
                + "\"client_email\":\"push@skultem-test.iam.gserviceaccount.com\","
                + "\"token_uri\":\"https://oauth2.googleapis.com/token\","
                + "\"private_key\":\"" + pem.replace("\n", "\\n") + "\"}";
    }

    @Test
    void offWithoutCredentials() {
        assertThat(new FcmPushService(mock(MobileDeviceRepository.class), "").isEnabled()).isFalse();
        assertThat(new FcmPushService(mock(MobileDeviceRepository.class), "{not json").isEnabled()).isFalse();
    }

    @Test
    void onWithAServiceAccountGivenInlineOrAsAFile() throws Exception {
        String json = serviceAccount();
        assertThat(new FcmPushService(mock(MobileDeviceRepository.class), json).isEnabled()).isTrue();

        var file = Files.createTempFile("firebase", ".json");
        Files.writeString(file, json);
        assertThat(new FcmPushService(mock(MobileDeviceRepository.class), file.toString()).isEnabled()).isTrue();
        Files.delete(file);
    }

    @Test
    @SuppressWarnings("unchecked")
    void messageCarriesTheAlertAndWhereATapGoes() {
        var n = new NotificationDTO("n1", "sch", "Exam results are out", "Second term results are ready.",
                Map.of(), null, null, false, Instant.now(), Instant.now());
        var m = FcmPushService.message("token-1", n);
        assertThat(m.get("token")).isEqualTo("token-1");
        assertThat((Map<String, Object>) m.get("notification"))
                .containsEntry("title", "Exam results are out")
                .containsEntry("body", "Second term results are ready.");
        assertThat((Map<String, String>) m.get("data"))
                .containsEntry("open", "alerts")
                .containsEntry("notificationId", "n1");
        var android = (Map<String, Object>) m.get("android");
        assertThat((Map<String, Object>) android.get("notification"))
                .containsEntry("channel_id", "school_alerts")
                .containsEntry("icon", "ic_stat_skultem");
    }
}
