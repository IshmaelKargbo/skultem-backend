package com.moriba.skultem.application.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.EmailCheckDTO;
import com.moriba.skultem.application.error.RuleException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;

// The one place Skultem decides whether an email address is acceptable - used by every form that saves an account
// email (users, parents, staff, school owners, platform admins) and by the live check behind the email fields.
//   1. Format: Jakarta Bean Validation's @Email (permissive on purpose - unusual but valid addresses pass), plus it
//      must have a domain with a dot.
//   2. Domain: DNS - MX records, or A/AAAA records as the fallback mail host. A lookup that fails (timeout, DNS down)
//      is UNCHECKED and never blocks saving: only a clear "this domain doesn't exist / takes no mail" does.
// Neither step proves the mailbox exists - that's what email verification (EmailVerificationService) is for. No
// SMTP probing.
@Service
public class EmailValidationService {

    private record Holder(@Email String value) {
    }

    private record Cached(MailDomainLookup.Result result, Instant at) {
    }

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final MailDomainLookup lookup;
    private final boolean domainCheckEnabled;
    private final Duration cacheFor;
    // Domains repeat constantly (gmail.com, yahoo.com) - remember answers for a while instead of asking DNS each time.
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public EmailValidationService(MailDomainLookup lookup,
            @Value("${email-validation.domain-check:true}") boolean domainCheckEnabled,
            @Value("${email-validation.cache-minutes:30}") long cacheMinutes) {
        this.lookup = lookup;
        this.domainCheckEnabled = domainCheckEnabled;
        this.cacheFor = Duration.ofMinutes(cacheMinutes);
    }

    /** Trimmed; null for null or blank (an optional email left empty). Case is kept - comparisons ignore it. */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        var trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean hasValidFormat(String email) {
        var value = normalize(email);
        if (value == null || value.length() > 254) {
            return false;
        }
        int at = value.lastIndexOf('@');
        if (at <= 0 || at == value.length() - 1) {
            return false;
        }
        String domain = value.substring(at + 1);
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".") || domain.contains("..")) {
            return false;
        }
        return VALIDATOR.validate(new Holder(value)).isEmpty();
    }

    public EmailCheckDTO check(String email) {
        var value = normalize(email);
        if (!hasValidFormat(value)) {
            return new EmailCheckDTO(value, "INVALID_FORMAT", false, "Enter a valid email address, like name@example.com.");
        }
        if (!domainCheckEnabled) {
            return new EmailCheckDTO(value, "UNCHECKED", true, null);
        }
        String domain = value.substring(value.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
        return switch (lookupDomain(domain)) {
            case ACCEPTS_MAIL -> new EmailCheckDTO(value, "OK", true, null);
            case NO_SUCH_DOMAIN -> new EmailCheckDTO(value, "NO_SUCH_DOMAIN", false,
                    "The domain " + domain + " doesn't exist. Check the spelling.");
            case NO_MAIL_SERVER -> new EmailCheckDTO(value, "NO_MAIL_SERVER", false,
                    domain + " can't receive email. Check the address.");
            case UNKNOWN -> new EmailCheckDTO(value, "UNCHECKED", true,
                    "We couldn't check this domain right now. The address can still be saved.");
        };
    }

    /**
     * For saving: the trimmed address (null when left empty), or a RuleException when it's clearly unusable - a bad
     * format, or a domain that doesn't exist or takes no mail. A domain that couldn't be checked is accepted.
     */
    public String requireAcceptable(String email) {
        var value = normalize(email);
        if (value == null) {
            return null;
        }
        var result = check(value);
        if (!result.deliverable()) {
            throw new RuleException(result.message());
        }
        return result.email();
    }

    private MailDomainLookup.Result lookupDomain(String domain) {
        var now = Instant.now();
        var hit = cache.get(domain);
        if (hit != null && hit.at().plus(cacheFor).isAfter(now)) {
            return hit.result();
        }
        var result = lookup.lookup(domain);
        // A failed lookup isn't remembered - the next attempt may well succeed.
        if (result != MailDomainLookup.Result.UNKNOWN) {
            if (cache.size() > 5_000) {
                cache.clear();
            }
            cache.put(domain, new Cached(result, now));
        }
        return result;
    }
}
