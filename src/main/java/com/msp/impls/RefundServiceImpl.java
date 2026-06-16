package com.msp.impls;

import com.msp.mappers.RefundMapper;
import com.msp.models.*;
import com.msp.payloads.dtos.RefundDto;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "refunds")
public class RefundServiceImpl implements RefundService {
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;

    @Override
    @Caching(
            put = {
                    @CachePut(key = "#result.id")
            },
            evict = {
                    @CacheEvict(value = "refunds-all", allEntries = true),
                    @CacheEvict(value = "refunds-by-cashier", allEntries = true),
                    @CacheEvict(value = "refunds-by-branch", allEntries = true),
                    @CacheEvict(value = "refunds-by-shift", allEntries = true),
                    @CacheEvict(value = "refunds-by-date-range", allEntries = true)
            }
    )
    public RefundDto createRefund(RefundDto refundDto) throws Exception {
        User cashier = userService.getCurrentUser();
        Order order = orderRepository.findById(refundDto.getOrderId()).orElseThrow(
                () -> new Exception("Order Not Found!")
        );
        Branch branch = order.getBranch();
        Refund createdRefund = Refund.builder()
                .order(order)
                .cashier(cashier)
                .branch(branch)
                .reason(refundDto.getReason())
                .amount(refundDto.getAmount())
                .createdAt(refundDto.getCreatedAt())
                .tenantId(cashier.getTenantId())
                .build();
        Refund savedRefund = refundRepository.save(createdRefund);
        return RefundMapper.toDto(savedRefund);
    }

    @Override
    @Cacheable(value = "refunds-all", key = "#page + '-' + #size")
    public Page<RefundDto> getAllRefunds(int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size);
        return refundRepository.findAll(pageable)
                .map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-by-cashier", key = "#cashierId + '-' + #page + '-' + #size")
    public Page<RefundDto> getRefundByCashier(UUID cashierId, int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size);
        return refundRepository.findByCashierId(cashierId, pageable)
                .map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-by-shift", key = "#shiftReportId + '-' + #page + '-' + #size")
    public Page<RefundDto> getRefundByShiftReport(UUID shiftReportId, int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size);
        return refundRepository.findByShiftReportId(shiftReportId, pageable)
                .map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-by-date-range", key = "#cashierId + '-' + #startDate + '-' + #endDate + '-' + #page + '-' + #size")
    public Page<RefundDto> getRefundByCashierAndDateRange(UUID cashierId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size);
        return refundRepository.findByCashierIdAndCreatedAtBetween(
                cashierId, startDate, endDate, pageable
        ).map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(value = "refunds-by-branch", key = "#branchId + '-' + #page + '-' + #size")
    public Page<RefundDto> getRefundByBranch(UUID branchId, int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size);
        return refundRepository.findByBranchId(branchId, pageable)
                .map(RefundMapper::toDto);
    }

    @Override
    @Cacheable(key = "#refundId")
    public RefundDto getRefundById(UUID refundId) throws Exception {
        return refundRepository.findById(refundId)
                .map(RefundMapper::toDto)
                .orElseThrow(() -> new Exception("Refund Not Found!"));
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(key = "#refundId"),
                    @CacheEvict(value = "refunds-all", allEntries = true),
                    @CacheEvict(value = "refunds-by-cashier", allEntries = true),
                    @CacheEvict(value = "refunds-by-branch", allEntries = true),
                    @CacheEvict(value = "refunds-by-shift", allEntries = true),
                    @CacheEvict(value = "refunds-by-date-range", allEntries = true)
            }
    )
    public void deleteRefund(UUID refundId) throws Exception {
        this.getRefundById(refundId);
        refundRepository.deleteById(refundId);
    }
}