package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SkullyParentServiceTest {

    private SkullyParentService service() {
        var s = new SkullyParentService(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(s, "defaultCountryCode", "232");
        return s;
    }

    @Test
    void aWhatsAppNumberMatchesTheWaysAParentPhoneIsSaved() {
        // WhatsApp sends 23277123456; schools save +232 77 123456, 77123456 or 077123456.
        assertThat(service().phoneForms("23277123456")).containsExactly("23277123456", "77123456", "077123456");
        assertThat(service().phoneForms("+232 77 123 456")).contains("23277123456", "077123456");
    }

    @Test
    void aForeignNumberIsOnlyMatchedAsIs() {
        assertThat(service().phoneForms("447700900123")).containsExactly("447700900123");
    }

    @Test
    void tooShortOrEmptyMatchesNothing() {
        assertThat(service().phoneForms("12345")).isEmpty();
        assertThat(service().phoneForms(null)).isEmpty();
    }
}
