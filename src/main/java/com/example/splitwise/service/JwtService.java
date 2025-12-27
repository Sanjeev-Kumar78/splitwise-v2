package com.example.splitwise.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * JwtService - implementation using Nimbus JOSE+JWT 10.6
 */
@Service
public class JwtService {

    private final byte[] secret;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expirationMs:36000000}") long expirationMs) {
        this.secret = secret.getBytes();
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, Map<String, Object> extraClaims) {
        try {
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + expirationMs));
            
            // Add extra claims
            for (Map.Entry<String, Object> entry : extraClaims.entrySet()) {
                builder.claim(entry.getKey(), entry.getValue());
            }
            
            JWTClaimsSet claimsSet = builder.build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(new MACSigner(secret));
            
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    public String generateToken(String subject) {
        return generateToken(subject, Map.of());
    }

    public String extractUsername(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Error extracting username from token", e);
        }
    }

    public boolean isTokenValid(String token, String username) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // Verify signature
            JWSVerifier verifier = new MACVerifier(secret);
            if (!signedJWT.verify(verifier)) {
                return false;
            }
            
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            // Check subject matches
            String subject = claims.getSubject();
            if (subject == null || !subject.equals(username)) {
                return false;
            }
            
            // Check expiration
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
