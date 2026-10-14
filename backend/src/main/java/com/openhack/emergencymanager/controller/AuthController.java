package com.openhack.emergencymanager.controller;

import com.openhack.emergencymanager.model.AppUser;
import com.openhack.emergencymanager.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    // A CSRF token is required for sign-in, registration, approval and every other POST/PATCH/DELETE.
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Message register(@RequestBody RegisterRequest request) {
        String fullName = request.fullName() == null ? "" : request.fullName().trim();
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        String password = request.password();
        if (fullName.isEmpty() || fullName.length() > 120 ||
            email.length() > 190 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") ||
            password == null || password.length() < 12 || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Name, valid email and password (12-128 characters) are required");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered");
        }
        users.save(new AppUser(fullName, email, encoder.encode(password),
            AppUser.Role.STAFF, AppUser.Status.PENDING));
        return new Message("Registration received. An administrator must approve your account.");
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        AppUser user = users.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return toResponse(user);
    }

    public static UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(),
            user.getRole().name(), user.getStatus().name());
    }

    public record RegisterRequest(String fullName, String email, String password) {}
    public record Message(String message) {}
    public record CsrfResponse(String headerName, String token) {}
    public record UserResponse(Long id, String fullName, String email, String role, String status) {}
}
