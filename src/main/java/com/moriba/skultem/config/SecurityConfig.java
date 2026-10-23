package com.moriba.skultem.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.moriba.skultem.infrastructure.security.JwtAuthFilter;
import com.moriba.skultem.infrastructure.security.JwtAuthenticationEntryPoint;
import com.moriba.skultem.infrastructure.security.RateLimitFilter;
import com.moriba.skultem.infrastructure.security.RateLimitFilter.Rule;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(
                        HttpSecurity http,
                        JwtAuthFilter authFilter,
                        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                        @Value("${rate-limit.enabled:true}") boolean rateLimitEnabled,
                        @Value("${rate-limit.client-ip-header:}") String clientIpHeader,
                        @Value("${rate-limit.login-per-minute:10}") int loginPerMinute,
                        @Value("${rate-limit.api-per-minute:300}") int apiPerMinute) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/demos").permitAll()
                                                .requestMatchers("/api/v1/school/**").permitAll()
                                                // Content for the marketing site: testimonials and blog posts to read,
                                                // and the contact form to send.
                                                .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/public/contact").permitAll()
                                                // Reachable pre-auth by design - see
                                                // BootstrapSystemAdminUseCase for how this stays safe
                                                // without a logged-in caller.
                                                .requestMatchers(HttpMethod.POST, "/api/v1/system/bootstrap")
                                                .permitAll()
                                                .requestMatchers("/ws/**").permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                                .httpBasic(basic -> basic.disable())
                                .formLogin(form -> form.disable());

                if (rateLimitEnabled) {
                        // After JwtAuthFilter, so logged-in calls are counted per user (see RateLimitFilter).
                        http.addFilterAfter(new RateLimitFilter(rateLimitRules(loginPerMinute, apiPerMinute),
                                        clientIpHeader), JwtAuthFilter.class);
                }

                return http.build();
        }

        // First match wins - the tight, anonymous ones (per client IP) before the general per-user limit.
        private static List<Rule> rateLimitRules(int loginPerMinute, int apiPerMinute) {
                return List.of(
                                // Password guessing.
                                new Rule("login", HttpMethod.POST, "/api/v1/auth/login", loginPerMinute, 60, false),
                                new Rule("admin-login", HttpMethod.POST, "/api/v1/auth/system-admin-login",
                                                loginPerMinute, 60, false),
                                new Rule("refresh", HttpMethod.POST, "/api/v1/auth/refresh", 30, 60, false),
                                new Rule("bootstrap", HttpMethod.POST, "/api/v1/system/bootstrap", 5, 3600, false),
                                // Website forms - spam.
                                new Rule("contact", HttpMethod.POST, "/api/v1/public/contact", 5, 600, false),
                                new Rule("demo", HttpMethod.POST, "/api/v1/demos", 5, 600, false),
                                // Everything else: per user when logged in, per IP otherwise.
                                new Rule("api", null, "/api/", apiPerMinute, 60, true));
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(List.of(
                                "http://localhost:3000",
                                // Every *.localhost host resolves to loopback (RFC 6761), which is how the
                                // frontend simulates subdomains locally - a school tenant (myschool.localhost)
                                // or the admin portal (admin.localhost) - see utils/tenant.ts. Without this,
                                // the browser's CORS preflight rejects those origins outright and every
                                // request from them (including login) fails before it reaches a controller.
                                "http://*.localhost:3000",
                                "http://*.localhost:3099",
                                "https://*.skultem.space",
                                // The marketing website - reads the public school directory.
                                "https://skultem.com",
                                "https://www.skultem.com"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(Arrays.asList(
                                "Content-Type",
                                "Authorization",
                                "Accept",
                                "Origin",
                                // Lets a retried enrollment/fee/payment request be recognised - see IdempotencyAspect.
                                "Idempotency-Key",
                                // Owner-level "view one section" choice - see SectionScopeService#view.
                                "X-View-Section"));
                // Retry-After: how long a rate-limited (429) client should wait - see RateLimitFilter.
                configuration.setExposedHeaders(List.of("Idempotent-Replay", "Retry-After"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
