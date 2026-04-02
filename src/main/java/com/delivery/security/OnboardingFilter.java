package com.delivery.security;

import com.delivery.entity.Role;
import com.delivery.entity.User;
import com.delivery.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_PATHS = List.of(
            "/api/company/onboard",
            "/api/company/profile",
            "/api/auth",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();

        // Skip if no auth
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip allowed paths
        if (ALLOWED_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        //  GET PRINCIPAL FROM JWT (NO DB)
        Object principalObj = authentication.getPrincipal();

        if (!(principalObj instanceof CustomUserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isCompany = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY"));

        if (isCompany && !principal.isOnboarded()) {

            log.warn("Blocked request for non-onboarded company user: {}", principal.getUsername());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");

            response.getWriter().write("""
            {
                "error": "COMPANY_NOT_ONBOARDED",
                "message": "Please complete onboarding first"
            }
            """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}