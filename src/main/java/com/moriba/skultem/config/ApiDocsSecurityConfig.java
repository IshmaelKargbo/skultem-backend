package com.moriba.skultem.config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// The API docs (Swagger UI at /docs, the OpenAPI spec at /v3/api-docs). Secure by default:
//   * off entirely unless API_DOCS_ENABLED=true - nothing is served, this config doesn't even load;
//   * behind their own login (HTTP Basic), separate from school accounts - API_DOCS_USERNAME / API_DOCS_PASSWORD,
//     with no default: switching the docs on without setting both stops the app from starting rather than
//     serving them open;
//   * 5 wrong passwords from one address locks it out for 15 minutes;
//   * "Try it out" stays off unless API_DOCS_SUBMIT_METHODS lists the methods to allow (see application.yaml) - and
//     even then every call still needs a real user's JWT, with that user's normal permissions.
// Runs before the main chain (SecurityConfig), and only for the docs paths.
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class ApiDocsSecurityConfig {

    private static final String[] DOCS_PATHS = { "/docs", "/docs/**", "/swagger-ui/**", "/v3/api-docs",
            "/v3/api-docs/**" };
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(15);

    @Bean
    @Order(1)
    public SecurityFilterChain apiDocsFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder,
            @Value("${api-docs.username:}") String username,
            @Value("${api-docs.password:}") String password) throws Exception {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "API docs are enabled (API_DOCS_ENABLED=true) but API_DOCS_USERNAME / API_DOCS_PASSWORD are not set"
                            + " - refusing to serve them without a login.");
        }
        if (password.length() < 16) {
            throw new IllegalStateException("API_DOCS_PASSWORD must be at least 16 characters.");
        }

        var docsUser = new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("API_DOCS")
                .build());

        http
                .securityMatcher(DOCS_PATHS)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("API_DOCS"))
                .userDetailsService(docsUser)
                .httpBasic(basic -> basic.realmName("Skultem API docs"))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new LoginThrottle(), BasicAuthenticationFilter.class)
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        .cacheControl(c -> {
                        }));
        return http.build();
    }

    // How the docs describe the API: every endpoint takes the same Bearer JWT the app uses.
    @Bean
    public OpenAPI skultemOpenApi(@Value("${api-docs.version:v1}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Skultem API")
                        .version(version)
                        .description("School management API. Every endpoint except /api/v1/auth/**, the public "
                                + "website endpoints and /api/v1/school/** needs a Bearer access token from "
                                + "POST /api/v1/auth/login; what it may do follows that user's roles in the active school."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    // Counts wrong passwords per client address and turns it away for LOCKOUT after MAX_FAILURES. Behind a reverse
    // proxy every client shares its address unless forwarded headers are honoured - then a guesser locks the docs
    // for everyone, which is the safe way round.
    static final class LoginThrottle extends OncePerRequestFilter {
        private record Attempts(int failures, Instant lockedUntil) {
        }

        private final Map<String, Attempts> byAddress = new ConcurrentHashMap<>();

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String address = request.getRemoteAddr();
            Attempts current = byAddress.get(address);
            if (current != null && current.lockedUntil() != null && Instant.now().isBefore(current.lockedUntil())) {
                // Written directly - sendError would forward to /error, which the main chain answers with a 401.
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("text/plain");
                response.getWriter().write("Too many failed logins - try again later");
                return;
            }

            chain.doFilter(request, response);

            boolean triedToLogIn = request.getHeader("Authorization") != null;
            if (response.getStatus() == HttpStatus.UNAUTHORIZED.value() && triedToLogIn) {
                byAddress.compute(address, (k, a) -> {
                    int failures = (a == null || a.lockedUntil() != null ? 0 : a.failures()) + 1;
                    return failures >= MAX_FAILURES ? new Attempts(0, Instant.now().plus(LOCKOUT))
                            : new Attempts(failures, null);
                });
            } else if (response.getStatus() < 400) {
                byAddress.remove(address);
            }
        }
    }
}
