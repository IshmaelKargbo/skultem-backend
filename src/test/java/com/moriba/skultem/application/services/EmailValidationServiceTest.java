package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.MailDomainLookup.Result;

class EmailValidationServiceTest {

    private final Map<String, Result> dns = new HashMap<>();
    private final AtomicInteger lookups = new AtomicInteger();
    private final EmailValidationService service = new EmailValidationService(domain -> {
        lookups.incrementAndGet();
        return dns.getOrDefault(domain, Result.ACCEPTS_MAIL);
    }, true, 30);

    @ParameterizedTest
    @ValueSource(strings = { "amina@example.com", "first.last@school.edu.sl", "name+tag@gmail.com",
            "o'brien@example.co.uk", "UPPER@Example.COM", "x@sub.domain.example.org", "  padded@example.com  " })
    void acceptsValidAddresses(String email) {
        assertThat(EmailValidationService.hasValidFormat(email)).isTrue();
        assertThat(service.check(email).status()).isEqualTo("OK");
    }

    @ParameterizedTest
    @ValueSource(strings = { "plainaddress", "missing-domain@", "@missing-local.com", "two@@example.com",
            "space in@example.com", "nodot@localhost", "dots@example..com", "trailing@example.com.",
            "leading@.example.com" })
    void rejectsMalformedAddresses(String email) {
        assertThat(EmailValidationService.hasValidFormat(email)).isFalse();
        var result = service.check(email);
        assertThat(result.status()).isEqualTo("INVALID_FORMAT");
        assertThat(result.deliverable()).isFalse();
        assertThat(lookups).hasValue(0);
    }

    @Test
    void emptyEmailIsAllowedWhereItIsOptional() {
        assertThat(service.requireAcceptable(null)).isNull();
        assertThat(service.requireAcceptable("   ")).isNull();
    }

    @Test
    void trimsWhitespace() {
        assertThat(service.requireAcceptable("  amina@example.com ")).isEqualTo("amina@example.com");
    }

    @Test
    void nonexistentDomainIsRejected() {
        dns.put("gmial.com", Result.NO_SUCH_DOMAIN);
        var result = service.check("amina@gmial.com");
        assertThat(result.status()).isEqualTo("NO_SUCH_DOMAIN");
        assertThat(result.deliverable()).isFalse();
        assertThatThrownBy(() -> service.requireAcceptable("amina@gmial.com"))
                .isInstanceOf(RuleException.class).hasMessageContaining("gmial.com");
    }

    @Test
    void domainWithoutMailServerIsRejected() {
        dns.put("nomail.example", Result.NO_MAIL_SERVER);
        assertThat(service.check("x@nomail.example").status()).isEqualTo("NO_MAIL_SERVER");
        assertThatThrownBy(() -> service.requireAcceptable("x@nomail.example")).isInstanceOf(RuleException.class);
    }

    @Test
    void dnsTimeoutDoesNotMakeAnAddressInvalid() {
        dns.put("slow.example", Result.UNKNOWN);
        var result = service.check("x@slow.example");
        assertThat(result.status()).isEqualTo("UNCHECKED");
        assertThat(result.deliverable()).isTrue();
        assertThat(service.requireAcceptable("x@slow.example")).isEqualTo("x@slow.example");
    }

    @Test
    void domainAnswersAreCachedButFailedLookupsAreRetried() {
        service.check("a@example.com");
        service.check("b@EXAMPLE.com");
        assertThat(lookups).hasValue(1);

        dns.put("flaky.example", Result.UNKNOWN);
        service.check("a@flaky.example");
        service.check("b@flaky.example");
        assertThat(lookups).hasValue(3);
    }

    @Test
    void domainCheckCanBeSwitchedOff() {
        var off = new EmailValidationService(domain -> { throw new AssertionError("no DNS when off"); }, false, 30);
        assertThat(off.check("x@anything.example").status()).isEqualTo("UNCHECKED");
        assertThat(off.check("bad").status()).isEqualTo("INVALID_FORMAT");
    }
}
