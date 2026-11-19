

package com.erwin.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private static final long EXPIRATION_MS = 1000L * 60 * 60; // 1 hora

    private final Key signingKey;

    public JwtService() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "La variable de entorno JWT_SECRET no está configurada. Debe contener una " +
                    "clave Base64 de al menos 32 bytes (ver .env.example). La aplicación no " +
                    "puede arrancar sin ella: firmar tokens con un secreto por defecto permitiría " +
                    "a cualquiera con el código fuente forjar sesiones válidas."
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    /** Genera el token de sesión del aplicativo. Los roles van como claim "roles"
     *  (cada uno ya en formato "ROLE_XXX"), NUNCA credenciales de base de datos. */
    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles != null ? roles : List.of())
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object raw = extractAllClaims(token).get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
