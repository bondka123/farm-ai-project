package com.farm.backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET = "mySuperSecretKeyThatIsVeryLongAndSecure123456";
    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // 🔥 GENERATE TOKEN (FIX IMPORTANT)
    public static String generateToken(String username, String role) {

        // 🔥 on garantit ROLE_
        String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return Jwts.builder()
                .setSubject(username)
                .claim("role", finalRole) // ✅ FIX ICI
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
                .signWith(key)
                .compact();
    }

    // 🔍 GET CLAIMS
    public static Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 👤 USERNAME
    public static String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    // 🔑 ROLE
    public static String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ✅ VALID TOKEN
    public static boolean isValid(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}