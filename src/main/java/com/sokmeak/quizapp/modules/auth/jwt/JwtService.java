package com.sokmeak.quizapp.modules.auth.jwt;


import com.sokmeak.quizapp.modules.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;

@Slf4j
@Service

public class JwtService {


    /** The claim holding "USER" or "ADMIN". "sub" already holds the username. */
    private static final String ROLE_CLAIM = "role";

    /** HS256 signs with a 256-bit key, and jjwt refuses anything shorter. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final JwtProperties properties;

    // JWT CONSTRUCTURE
    public JwtService(JwtProperties properties) {
        byte[] keyBytes = properties.secret() == null
                ? new byte[0]
                : properties.secret().getBytes(StandardCharsets.UTF_8);

        // Fail at startup, not on the first login attempt in production.
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES + " characters (set the JWT_SECRET "
                            + "environment variable); got " + keyBytes.length);
        }

        this.signingKey = hmacShaKeyFor(keyBytes);
        this.properties = properties;
    }


    public String issue(User user){
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + properties.expiration().toMillis());

        // The issuer must be stamped here: toAuthentication() calls requireIssuer(),
        // so a token without it is rejected on the very next request.
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(properties.issuer())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    public UsernamePasswordAuthenticationToken toAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                // Signature and expiry are both checked here; an expired token throws.
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();
        String role = claims.get(ROLE_CLAIM, String.class);
        if (username == null || role == null) {
            throw new JwtException("Token is missing its subject or role claim");
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        // null credentials: the password is not in the token and must never be.
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }

}

