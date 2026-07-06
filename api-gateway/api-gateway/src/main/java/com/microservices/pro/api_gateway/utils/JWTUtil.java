package com.microservices.pro.api_gateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import static io.netty.util.CharsetUtil.UTF_8;

@Component
public class JWTUtil {
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(UTF_8));
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody(); // throws if invalid or expired
    }

}
