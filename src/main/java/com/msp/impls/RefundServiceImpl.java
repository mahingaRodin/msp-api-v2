package com.msp.impls;

import com.msp.enums.EOrderStatus;
import com.msp.enums.ERefundStatus;
import com.msp.enums.EUserRole;
import com.msp.exceptions.UserException;
import com.msp.mappers.RefundMapper;
import com.msp.models.*;
import com.msp.payloads.dtos.RefundDto;
import com.msp.repositories.InventoryRepository;
import com.msp.repositories.OrderRepository;
import com.msp.repositories.RefundRepository;
import com.msp.services.RefundService;
import com.msp.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "refunds")
public class RefundServiceImpl implements RefundService {
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Caching(
            put = { @CachePut(key = "#result.id") },
            evict = {
                    @CacheEvict(value = "refunds-all", allEntries = true),
                    @CacheEvict(value = "refunds-by-cashier", allEntries = true),
                    @CacheEvict(value = "refunds-by-branch", allEntries = true),
                    @CacheEvict(value = "refunds-by-shift", allEntries = true),
                    @CacheEvict(value = "refunds-by-date-range", allEntries = true)
            }
    )
    @Transactional
    public RefundDto createRefund(RefundDto refundDto) throws Exception {
        User cashier = userService.getCurrentUser();
        Order order = orderRepository.findByIdWithItems(refundDto.getOrderId()).orElseThrow(
                () -> new Exception("Order Not Found!")
        );
        Branch branch = order.getBranch();
        restockOrderItems(order);
        order.setStatus(EOrderStatus.REFUNDED);
        orderRepository.save(order);

        Refund createdRefund = Refund.builder()
                .order(order)
                .cashier(cashier)
                .branch(branch)
                .reason(refundDto.getReason())
                .amount(refundDto.getAmount() != null ? refundDto.getAmount() : order.getTotalAmount())
                .paymentType(order.getPaymentType())
                .status(ERefundStatus.COMPLETED)
                .restocked(true)
                .tenantId(cashier.getTenantId())
                .build();
        return RefundMapper.toDto(refundRepository.save(createdRefund));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "refunds-all", allEntries = true),
            @CacheEvict(value = "refunds-by-branch", allEntries = true)
    })
    public RefundDto requestCustomerRefund(UUID orderId, String reason) throws Exception {
        User user = userService.getCurrentUser();
        if (user.getRole() != EUserRole.ROLE_CUSTOMER) {
            throw new UserException("Only customers can request refunds here");
        }
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new UserException("Order not found"));
        if (order.getCustomer() == null || order.getCustomer().getEmail() == null
                || !order.getCustomer().getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new UserException("You can only request refunds for your own orders");
        }
        if (order.getStatus() == EOrderStatus.REFUNDED || order.getStatus() == EOrderStatus.CANCELLED) {
            throw new UserException("This order is not eligible for a refund");
        }
        if (refundRepository.existsByOrderIdAndStatusIn(orderId,
                List.of(ERefundStatus.PENDING_RETURN, ERefundStatus.COMPLETED))) {
            throw new UserException("A refund request already exists for this order");
        }
        Refund refund = Refund.builder()
                .order(order)
                .branch(order.getBranch())
                .reason(reason != null && !reason.isBlank() ? reason : "Customer return request")
                .amount(order.getTotalAmount())
                .paymentType(order.getPaymentType())
                .status(ERefundStatus.PENDING_RETURN)
                .restocked(false)
                .requestedBy(user)
                .tenantId(order.getTenantId())
                .build();
        return RefundMapper.toDto(refundRepository.save(refund));
    }

    @Override
    @Transactional
    @Caching(put = { @CachePut(key = "#result.id") }, evict = {
            @CacheEvict(value = "refunds-all", allEntries = true),
            @CacheEvict(value = "refunds-by-branch", allEntries = true),
            @CacheEvict(value = "refunds-by-cashier", allEntries = true)
    })
    public RefundDto approveReturnAndRefund(UUID refundId) throws Exception {
        User cashier = userService.getCurrentUser();
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new UserException("Refund request not found"));
        if (refund.getStatus() != ERefundStatus.PENDING_RETURN) {
            throw new UserException("Only pending return requests can be approved");
        }
        Order order = orderRepository.findByIdWithItems(refund.getOrder().getId())
                .orElseThrow(() -> new UserException("Order not found"));
        restockOrderItems(order);
        order.setStatus(EOrderStatus.REFUNDED);
        orderRepository.save(order);

        refund.setCashier(cashier);
        refund.setStatus(ERefundStatus.COMPLETED);
        refund.setRestocked(true);
        refund.setAmount(order.getTotalAmount());
        return RefundMapper.toDto(refundRepository.save(refund));
    }

    @Override
    @Transactional
    public RefundDto rejectRefundRequest(UUID refundId, String reason) throws Exception {
        User cashier = userService.getCurrentUser();
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new UserException("Refund request not found"));
        if (refund.getStatus() != ERefundStatus.PENDING_RETURN) {
            throw new UserException("Only pending return requests can be rejected");
        }
        refund.setCashier(cashier);
        refund.setStatus(ERefundStatus.REJECTED);
        if (reason != null && !reason.isBlank()) {
            refund.setReason(reason);
        }
        return RefundMapper.toDto(refundRepository.save(refund));
    }

    private void restockOrderItems(Order order) {
        if (order.getItems() == null || order.getBranch() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) continue;
            Inventory inv = inventoryRepository.findByProductIdAndBranchId(
                    item.getProduct().getId(), order.getBranch().getId());
            if (inv != null) {
                inv.setQuantity((inv.getQuantity() != null ? inv.getQuantity() : 0)
                        + (item.getQuantity() != null ? item.getQuantity() : 0));
                inventoryRepository.save(inv);
            }
        }
    }

    @Override
    public Page<RefundDto> getPendingReturnsForBranch(UUID branchId, int page, int size) {
        return refundRepository.findByBranchIdAndStatus(branchId, ERefundStatus.PENDING_RETURN, PageRequest.of(page, size))
                .map(RefundMapper::toDto);
    }

    @Override
    public Page<RefundDto> getMyRefundRequests(int page, int size) {
        User user = userService.getCurrentUser();
        return refundRepository.findByRequestedById(user.getId(), PageRequest.of(page, size))
                .map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-all", key = "#page + '-' + #size")
    public Page<RefundDto> getAllRefunds(int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size);
        return refundRepository.findAll(pageable).map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-by-cashier", key = "#cashierId + '-' + #page + '-' + #size")
    public Page<RefundDto> getRefundByCashier(UUID cashierId, int page, int size) throws Exception {
        return refundRepository.findByCashierId(cashierId, PageRequest.of(page, size)).map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-by-shift", key = "#shiftReportId + '-' + #page + '-' + #size")
    public Page<RefundDto> getRefundByShiftReport(UUID shiftReportId, int page, int size) throws Exception {
        return refundRepository.findByShiftReportId(shiftReportId, PageRequest.of(page, size)).map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-by-date-range", key = "#cashierId + '-' + #startDate + '-' + #endDate + '-' + #page + '-' + #size")
    public Page<RefundDto> getRefundByCashierAndDateRange(UUID cashierId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) throws Exception {
        return refundRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, startDate, endDate, PageRequest.of(page, size)
        ).map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-by-branch", key = "#branchId + '-' + #page + '-' + #size")
    public Page<RefundDto> getRefundByBranch(UUID branchId, int page, int size) throws Exception {
        return refundRepository.findByBranchId(branchId, PageRequest.of(page, size)).map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(key = "#refundId")
    public RefundDto getRefundById(UUID refundId) throws Exception {
        return refundRepository.findById(refundId)
                .map(RefundMapper::toDto)
                .orElseThrow(() -> new Exception("Refund Not Found!"));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(key = "#refundId"),
            @CacheEvict(value = "refunds-all", allEntries = true),
            @CacheEvict(value = "refunds-by-cashier", allEntries = true),
            @CacheEvict(value = "refunds-by-branch", allEntries = true),
            @CacheEvict(value = "refunds-by-shift", allEntries = true),
            @CacheEvict(value = "refunds-by-date-range", allEntries = true)
    })
    public void deleteRefund(UUID refundId) throws Exception {
        this.getRefundById(refundId);
        refundRepository.deleteById(refundId);
    }
}
