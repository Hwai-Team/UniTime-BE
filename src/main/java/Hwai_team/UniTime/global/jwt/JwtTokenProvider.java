// src/main/java/Hwai_team/UniTime/global/jwt/JwtTokenProvider.java
package Hwai_team.UniTime.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityInMs;
    private final long refreshTokenValidityInMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            // 기본 1시간
            @Value("${jwt.access-token-validity-ms:3600000}") long accessTokenValidityInMs,
            // 기본 14일
            @Value("${jwt.refresh-token-validity-ms:1209600000}") long refreshTokenValidityInMs
    ) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMs = accessTokenValidityInMs;
        this.refreshTokenValidityInMs = refreshTokenValidityInMs;
    }

    // ===== 토큰 생성 =====

    public String generateAccessToken(String email) {
        return buildToken(email, accessTokenValidityInMs);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, refreshTokenValidityInMs);
    }

    private String buildToken(String email, long validityInMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMs);

        return Jwts.builder()
                .setSubject(email)          // 토큰 주인 = 이메일
                .setIssuedAt(now)           // 발급 시간
                .setExpiration(expiry)      // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ===== 토큰에서 정보 꺼내기 =====

    public String getUserEmail(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    // ===== 토큰 유효성 검증 =====

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = parseClaims(token);
            Date expiration = claims.getBody().getExpiration();
            return expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치, 만료, 포맷 오류 등
            return false;
        }
    }
}