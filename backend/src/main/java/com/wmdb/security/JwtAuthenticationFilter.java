package com.wmdb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            if (jwtUtils.isTokenValid(jwt)) {
                String idCard = jwtUtils.extractIdCard(jwt);
                String realName = jwtUtils.extractRealName(jwt);

                // In a real app, you might want to load UserDetails and authorities here.
                // For simplicity, we just set the principal to the ID card.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        idCard, null, new ArrayList<>());

                // You could store realName in details or context if needed.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token is invalid, let the request proceed to hit authorization rules
        }

        filterChain.doFilter(request, response);
    }
}
