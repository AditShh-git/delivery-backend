package com.delivery.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = "anonymousUser";
        String roles    = "ROLE_ANONYMOUS";

        if (authentication != null) {
            username = authentication.getName();
            roles    = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(", "));
        }

        log.warn("Access denied — user: {} | roles: {} | path: {} {}",
                username, roles,
                request.getMethod(),
                request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        response.getWriter().write("""
                {
                  "status": 403,
                  "error": "Forbidden",
                  "message": "You do not have permission to access this resource",
                  "user": "%s",
                  "roles": "%s",
                  "path": "%s"
                }
                """.formatted(username, roles, request.getRequestURI()));
    }
}