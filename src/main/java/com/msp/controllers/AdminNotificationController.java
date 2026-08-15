package com.msp.controllers;

import com.msp.models.AdminNotification;
import com.msp.repositories.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationRepository repo;

    @GetMapping
    public ResponseEntity<Page<AdminNotification>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(repo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)));
    }

    @GetMapping("/ticker")
    public ResponseEntity<Map<String, Object>> ticker() {
        List<AdminNotification> unread = repo.findTop20ByReadFalseOrderByCreatedAtDesc();
        return ResponseEntity.ok(Map.of(
                "unreadCount", repo.countByReadFalse(),
                "items", unread
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        repo.findById(id).ifPresent(n -> {
            n.setRead(true);
            repo.save(n);
        });
        return ResponseEntity.ok().build();
    }
}
