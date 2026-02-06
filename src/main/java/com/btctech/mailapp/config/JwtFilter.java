package com.btctech.mailapp.config;

import com.btctech.mailapp.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (JwtException | IllegalArgumentException e) {
                log.error("Invalid JWT Token: {}", e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // FIX: Handle temp_ tokens by stripping the prefix
            String actualUsername = username;
            boolean isTempToken = false;
            
            if (username.startsWith("temp_")) {
                actualUsername = username.substring(5); // Remove "temp_" prefix
                isTempToken = true;
                log.debug("Processing temp token for user: {}", actualUsername);
            }

            try {
                var user = userService.getUserByEmailOrUsername(actualUsername);
                
                // For temp tokens, validate against the prefixed username
                // For regular tokens, validate against the actual username
                String tokenUsername = isTempToken ? username : user.getUsername();
                
                if (jwtUtil.validateToken(jwt, tokenUsername)) {
                    // Create simple authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user.getUsername(), null, new ArrayList<>());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated user: {} (temp: {})", actualUsername, isTempToken);
                }
            } catch (Exception e) {
                log.error("User validation failed during JWT filter: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}