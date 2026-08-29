package cn.nanpo.window.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cn.nanpo.window.infrastructure.persistence.AuthRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthRepository repository;
    private final TokenCodec tokenCodec;

    public BearerTokenAuthenticationFilter(AuthRepository repository, TokenCodec tokenCodec) {
        this.repository = repository;
        this.tokenCodec = tokenCodec;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String rawToken = authorization.substring(7);
            repository.findActiveUserByAccessHash(tokenCodec.sha256(rawToken))
                    .ifPresent(principal -> {
                        var authorities = principal.roles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();
                        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                                principal, rawToken, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }
}

