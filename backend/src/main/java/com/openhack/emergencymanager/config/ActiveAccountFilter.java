package com.openhack.emergencymanager.config;

import com.openhack.emergencymanager.model.AppUser;
import com.openhack.emergencymanager.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Rechecks account status so admin deactivation invalidates existing sessions on their next API call. */
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final AppUserRepository users;

    public ActiveAccountFilter(AppUserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (req.getRequestURI().startsWith("/api/")
                && auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            boolean active = users.findByEmailIgnoreCase(auth.getName())
                .map(user -> user.getStatus() == AppUser.Status.ACTIVE)
                .orElse(false);
            if (!active) {
                new SecurityContextLogoutHandler().logout(req, resp, auth);
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"message\":\"Account disabled. Sign in again after approval.\"}");
                return;
            }
        }
        chain.doFilter(req, resp);
    }
}
