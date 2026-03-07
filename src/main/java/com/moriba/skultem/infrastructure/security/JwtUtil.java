package com.moriba.skultem.infrastructure.security;

import java.security.Key;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.moriba.skultem.domain.model.Role;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final Key key;
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
                .setSubject(userId)
                .claim("sch", schoolId)
                .claim("sid", sessionId)
                .claim("roles", roleNames)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String sessionId) {
        return Jwts.builder()
                .setSubject(sessionId)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
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

        return parseClaims(token).getSubject();
    }

    public String extractSessionId(String token) {
        Claims claims = parseClaims(token);
        if ("refresh".equals(claims.get("type", String.class))) {
            return claims.getSubject();
        }

        return parseClaims(token).get("sid", String.class);
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