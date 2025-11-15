package com.mychess.my_chess_backend.services.auth;

import com.mychess.my_chess_backend.models.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;
    @Getter
    @Value("${security.jwt.expiration-time}")
    private Duration jwtExpiration;

    public String extractEmail(String token)  { return extractClaim(token, Claims::getSubject); }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    public String generateToken(User user) { return this.generateToken(new HashMap<>(), user); }
    public String generateToken( Map<String, Object> extraClaims, User user ) {
//        extraClaims.put("email", user.getEmail());
        return this.buildToken(extraClaims, user, this.jwtExpiration);
    }
    public boolean isTokenValid(String token, User user) {
        final String email = this.extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }
    public boolean isTokenExpired(String token) { return this.extractExpiration(token).before(new Date()); }

    private Date extractExpiration(String token) { return this.extractClaim(token, Claims::getExpiration); }
    private String buildToken(Map<String, Object> extraClaims, User user, Duration expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration.toMillis()))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();
    }
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(this.getSignInKey()).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) { return e.getClaims(); }
    }
    private Key getSignInKey() {
        byte[] keyInBytes = Decoders.BASE64.decode(this.secretKey);
        return Keys.hmacShaKeyFor(keyInBytes);
    }
}
