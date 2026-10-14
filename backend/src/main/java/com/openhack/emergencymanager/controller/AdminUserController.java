package com.openhack.emergencymanager.controller;

import com.openhack.emergencymanager.model.AppUser;
import com.openhack.emergencymanager.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AppUserRepository users;
    public AdminUserController(AppUserRepository users) { this.users = users; }

    @GetMapping
    public List<AuthController.UserResponse> list() {
        return users.findAllByOrderByCreatedAtDesc().stream()
            .map(AuthController::toResponse).toList();
    }

    @PostMapping("/{id}/{action}")
    @Transactional
    public AuthController.UserResponse update(@PathVariable Long id, @PathVariable String action) {
        AppUser user = users.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // Admin identities cannot be altered via this endpoint.
        if (user.getRole() == AppUser.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator accounts cannot be modified here");
        }
        switch (action) {
            case "approve" -> {
                if (user.getStatus() != AppUser.Status.PENDING && user.getStatus() != AppUser.Status.DISABLED)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot approve this account");
                user.changeStatus(AppUser.Status.ACTIVE);
            }
            case "reject" -> {
                if (user.getStatus() != AppUser.Status.PENDING)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending requests can be rejected");
                user.changeStatus(AppUser.Status.REJECTED);
            }
            case "disable" -> {
                if (user.getStatus() != AppUser.Status.ACTIVE)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active users can be disabled");
                user.changeStatus(AppUser.Status.DISABLED);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action");
        }
        return AuthController.toResponse(user);
    }
}
