package com.rydr.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT utility (JJWT 0.12+/0.13 API).
 *
 * @author oi
 */
public class JwtUtil {

    /**
     * Secret key, stored only on the server side.
     * IMPORTANT: Override via environment variable JWT_SECRET in production.
     */
    private static String secret = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET") : "changeme-override-in-production";

    /**
     * The signing key is derived from the secret with SHA-256, so secrets of any length
     * produce a valid 256-bit HMAC key (HS256).
     */
    private static final SecretKey SIGN_KEY = buildSignKey(secret);

    private static SecretKey buildSignKey(String secretValue) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secretValue.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 message digest is not available", e);
        }
    }

    /**
     * Create a JWT token.
     *
     * @param subject   the subject claim
     * @param issueDate the token issue date
     * @return the signed JWT token string
     */
    public static String createToken(String subject, Date issueDate) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(issueDate)
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(SIGN_KEY)
                .compact();
    }

    /**
     * Parse and validate a JWT token.
     *
     * @param token the JWT token to parse
     * @return the subject claim, or empty string if invalid/expired
     */
    public static String parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(SIGN_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (claims != null) {
                return claims.getSubject();
            }
        } catch (ExpiredJwtException e) {
            System.out.println("JWT token has expired");
        } catch (Exception e) {
            // Malformed, unsigned or tampered token: treat as unauthenticated instead of throwing
            System.out.println("Invalid JWT token: " + e.getMessage());
        }

        return "";
    }

    public static void main(String[] args) {
        String subject = "1";
        String token = createToken(subject, new Date());
        System.out.println(token);
        try {
            Thread.sleep(10010);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Original value: " + parseToken(token));

    }

}
