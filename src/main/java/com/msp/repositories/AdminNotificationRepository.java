package com.msp.repositories;

import com.msp.models.AdminNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, UUID> {
    Page<AdminNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<AdminNotification> findTop20ByReadFalseOrderByCreatedAtDesc();
    long countByReadFalse();
}
