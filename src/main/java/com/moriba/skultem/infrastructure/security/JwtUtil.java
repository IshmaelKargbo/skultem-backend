package com.moriba.skultem.infrastructure.security;

import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.moriba.skultem.domain.model.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-expiration}") long accessExpiration,
            @Value("${security.jwt.refresh-expiration}") long refreshExpiration) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(String userId, String schoolId, List<Role> roles, String sessionId) {
        List<String> roleNames = roles.stream().map(Role::name).toList();

        return Jwts.builder()
                .subject(userId)
                .claim("sch", schoolId)
                .claim("sid", sessionId)
                .claim("roles", roleNames)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String sessionId) {
        return Jwts.builder()
                .subject(sessionId)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateAccessToken(String token, String userId) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(userId) && !isTokenExpired(claims);
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token, String sessionId) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class)) && claims.getSubject().equals(sessionId)
                    && !isTokenExpired(claims);
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        Claims claims = parseClaims(token);
        if ("refresh".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Cannot extract userId from refresh token");
        }
        return claims.getSubject();
    }

    public String extractSessionId(String token) {
        Claims claims = parseClaims(token);
        if ("refresh".equals(claims.get("type", String.class))) {
            return claims.getSubject();
        }
        return claims.get("sid", String.class);
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return "refresh".equals(claims.get("type", String.class));
    }

    public String extractSessionIdFromRefresh(String token) {
        if (!isRefreshToken(token)) {
            throw new IllegalArgumentException("Not a refresh token");
        }
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) parseClaims(token).get("roles");
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}