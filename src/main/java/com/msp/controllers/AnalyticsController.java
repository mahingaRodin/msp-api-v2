package com.msp.controllers;

import com.msp.payloads.dtos.AnalyticsSummaryDto;
import com.msp.payloads.dtos.DailyMetricDto;
import com.msp.payloads.dtos.NamedMetricDto;
import com.msp.repositories.BranchRepository;
import com.msp.repositories.InventoryRepository;
import com.msp.repositories.OrderRepository;
import com.msp.repositories.ProductRepository;
import com.msp.repositories.StoreRepository;
import com.msp.repositories.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Live revenue and order metrics from recorded sales")
public class AnalyticsController {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @GetMapping("/platform")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<AnalyticsSummaryDto> platform() {
        LocalDateTime from = LocalDate.now().minusDays(13).atStartOfDay();
        return ResponseEntity.ok(AnalyticsSummaryDto.builder()
                .revenue(nz(orderRepository.sumPlatformRevenue()))
                .orderCount(orderRepository.countPlatformOrders())
                .storeCount(storeRepository.count())
                .branchCount(branchRepository.count())
                .productCount(productRepository.count())
                .daily(mapDaily(orderRepository.dailyPlatform(from)))
                .byBranch(mapNamed(orderRepository.revenueByBranch()))
                .topProducts(mapNamed(orderRepository.topProducts()).stream().limit(8).toList())
                .build());
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_STORE_ADMIN','ROLE_STORE_MANAGER')")
    public ResponseEntity<AnalyticsSummaryDto> store(@PathVariable UUID storeId) {
        LocalDateTime from = LocalDate.now().minusDays(13).atStartOfDay();
        long branches = branchRepository.findByStoreId(storeId, org.springframework.data.domain.PageRequest.of(0, 1))
                .getTotalElements();
        long products = productRepository.findByStoreId(storeId, org.springframework.data.domain.PageRequest.of(0, 1))
                .getTotalElements();
        return ResponseEntity.ok(AnalyticsSummaryDto.builder()
                .revenue(nz(orderRepository.sumStoreRevenue(storeId)))
                .orderCount(orderRepository.countStoreOrders(storeId))
                .storeCount(1)
                .branchCount(branches)
                .productCount(products)
                .daily(mapDaily(orderRepository.dailyStore(storeId, from)))
                .byBranch(mapNamed(orderRepository.revenueByBranchForStore(storeId)))
                .topProducts(mapNamed(orderRepository.topProductsByStore(storeId)).stream().limit(8).toList())
                .build());
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_BRANCH_MANAGER')")
    public ResponseEntity<AnalyticsSummaryDto> branch(@PathVariable UUID branchId) {
        LocalDateTime from = LocalDate.now().minusDays(13).atStartOfDay();
        long cashiers = userRepository.countByBranchIdAndRole(branchId, com.msp.enums.EUserRole.ROLE_BRANCH_CASHIER);
        long inventorySkus = inventoryRepository.countByBranchId(branchId);
        return ResponseEntity.ok(AnalyticsSummaryDto.builder()
                .revenue(nz(orderRepository.sumBranchRevenue(branchId)))
                .orderCount(orderRepository.countBranchOrders(branchId))
                .storeCount(1)
                .branchCount(1)
                .productCount(inventorySkus)
                .employeeCount(cashiers)
                .daily(mapDaily(orderRepository.dailyBranch(branchId, from)))
                .byBranch(List.of())
                .topProducts(mapNamed(orderRepository.topProductsByBranch(branchId)).stream().limit(8).toList())
                .build());
    }

    private static double nz(Double v) {
        return v == null ? 0 : v;
    }

    private static List<DailyMetricDto> mapDaily(List<Object[]> rows) {
        return rows.stream().map(r -> DailyMetricDto.builder()
                .day(String.valueOf(r[0]))
                .revenue(r[1] instanceof Number n ? n.doubleValue() : 0)
                .orders(r[2] instanceof Number n ? n.longValue() : 0)
                .build()).collect(Collectors.toList());
    }

    private static List<NamedMetricDto> mapNamed(List<Object[]> rows) {
        return rows.stream().map(r -> NamedMetricDto.builder()
                .name(r[0] == null ? "Unknown" : String.valueOf(r[0]))
                .value(r[1] instanceof Number n ? n.doubleValue() : 0)
                .build()).collect(Collectors.toList());
    }
}
