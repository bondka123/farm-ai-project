package com.farm.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("🚀 JWT FILTER - Request: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println("🚀 JWT FILTER - Auth Header: " + (authHeader != null ? "PRESENT" : "MISSING"));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            try {

                if (JwtUtil.isValid(token)) {

                    String username = JwtUtil.getUsername(token);
                    String role = JwtUtil.getRole(token);

                    // 🔥 CORRECTION ICI
                    String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    
                    System.out.println("✅ JWT FILTER - Token Valid! User: " + username + " | Role: " + finalRole);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(finalRole))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    System.out.println("❌ JWT FILTER - Token Invalid or Expired!");
                }

            } catch (Exception e) {
                System.out.println("❌ JWT ERROR: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}