package com.QHSEAnalytics.auth.controller;

import com.QHSEAnalytics.auth.dto.request.CreateAnalysteRequest;
import com.QHSEAnalytics.auth.dto.request.UpdateUserRequest;
import com.QHSEAnalytics.auth.dto.response.MessageResponse;
import com.QHSEAnalytics.auth.dto.response.UserListResponse;
import com.QHSEAnalytics.auth.dto.response.UserResponse;
import com.QHSEAnalytics.auth.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminUserService.getAllUsers(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createAnalyste(@Valid @RequestBody CreateAnalysteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createAnalyste(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.deleteUser(id));
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<UserResponse> verifyUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.verifyUser(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.activateUser(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.deactivateUser(id));
    }

    @PatchMapping("/{id}/promote")
    public ResponseEntity<UserResponse> promoteToAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.promoteToAdmin(id));
    }

    @PatchMapping("/{id}/demote")
    public ResponseEntity<UserResponse> demoteToAnalyste(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.demoteToAnalyste(id));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.adminResetPassword(id));
    }
}