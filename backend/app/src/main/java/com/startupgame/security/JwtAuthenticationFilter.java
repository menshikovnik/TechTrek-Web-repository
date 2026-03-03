package com.startupgame.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.startupgame.modules.user.User;
import com.startupgame.modules.user.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper; // лучше инжектить

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = resolveBearerToken(request);
                if (token != null) {
                    processToken(token, request);
                }
            }

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            writeUnauthorized(response, "Token expired", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    private void processToken(String token, HttpServletRequest request) {
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        if ("ROLE_GUEST".equals(role)) {
            authenticateGuest(username, token, role, request);
        } else {
            authenticateUser(username, token, request);
        }
    }

    private void authenticateGuest(String username,
                                   String token,
                                   String role,
                                   HttpServletRequest request) {

        if (!jwtUtil.validateToken(token)) {
            return;
        }

        MDC.put("username", username);

        var authorities = List.of(new SimpleGrantedAuthority(role));
        setAuthentication(username, authorities, request);
    }

    private void authenticateUser(String username,
                                  String token,
                                  HttpServletRequest request) {

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtUtil.validateToken(token, userDetails)) {
            return;
        }

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        MDC.put("userId", String.valueOf(user.getId()));
        MDC.put("username", user.getUsername());

        setAuthentication(userDetails, userDetails.getAuthorities(), request);
    }

    private void setAuthentication(Object principal,
                                   Collection<? extends GrantedAuthority> authorities,
                                   HttpServletRequest request) {

        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    private void writeUnauthorized(HttpServletResponse response,
                                   String error,
                                   String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, String> body = Map.of(
                "error", error,
                "message", message
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}