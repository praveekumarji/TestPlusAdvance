package com.testpulse.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public JwtAuthenticationToken(String userId, Collection<? extends GrantedAuthority> authorities) {
        super(userId, null, authorities);
    }
}