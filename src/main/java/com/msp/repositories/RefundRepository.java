package com.msp.repositories;

import com.msp.enums.ERefundStatus;
import com.msp.models.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    Page<Refund> findByCashierIdAndCreatedAtBetween(
            UUID cashierId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    List<Refund> findByCashierIdAndCreatedAtBetween(
            UUID cashierId,
            LocalDateTime from,
            LocalDateTime to
    );

    Page<Refund> findByCashierId(UUID cashierId, Pageable pageable);
    Page<Refund> findByShiftReportId(UUID shiftReportId, Pageable pageable);
    Page<Refund> findByBranchId(UUID branchId, Pageable pageable);
    Page<Refund> findByBranchIdAndStatus(UUID branchId, ERefundStatus status, Pageable pageable);
    Page<Refund> findByRequestedById(UUID userId, Pageable pageable);
    boolean existsByOrderIdAndStatusIn(UUID orderId, List<ERefundStatus> statuses);
}
