package com.delivery.security;

import com.delivery.entity.Rider;
import com.delivery.entity.Role;
import com.delivery.entity.User;
import com.delivery.repository.RiderRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final RiderRepository riderRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @PostConstruct
    public void init() {
        log.info("JWT initialized — expiration: {}ms", jwtExpirationMs);
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generateToken(User user) {

        List<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .toList();

        boolean onboarded = user.getCompany() != null
                && user.getCompany().isOnboarded();

        Long companyId = user.getCompany() != null
                ? user.getCompany().getId()
                : null;

        String zone = user.getCompany() != null
                ? user.getCompany().getZone()
                : null;

        // ── riderOnDutySnapshot claim ─────────────────────────────────────
        // SNAPSHOT AT LOGIN TIME — for UI display only (show/hide shift toggle).
        // NEVER use this for dispatch, availability gating, or any business logic.
        // Dispatch MUST always call rider.isOnDuty() from DB.
        // Naming convention: "Snapshot" suffix = stale-aware, UI-only.
        Boolean riderOnDutySnapshot = null;
        boolean isRider = user.getRoles().stream()
                .anyMatch(r -> Role.RIDER.equals(r.getName()));
        if (isRider) {
            riderOnDutySnapshot = riderRepository.findByUserId(user.getId())
                    .map(Rider::getIsOnDuty)
                    .orElse(false);
            log.debug("JWT — riderOnDutySnapshot={} for user {} (UI-only hint)",
                    riderOnDutySnapshot, user.getEmail());
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("roles", roles)
                .claim("onboarded", onboarded)
                .claim("companyId", companyId)
                .claim("zone", zone)
                .claim("riderOnDutySnapshot", riderOnDutySnapshot)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            boolean valid   = expiration.after(new Date());
            log.debug("Token valid: {} | expires: {}", valid, expiration);
            return valid;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token invalid: {}", e.getMessage());
            return false;
        }
    }
}