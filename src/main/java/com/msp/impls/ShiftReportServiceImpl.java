package com.msp.impls;

import com.msp.enums.EPaymentType;
import com.msp.mappers.ShiftReportMapper;
import com.msp.models.*;
import com.msp.payloads.dtos.ShiftReportDto;
import com.msp.repositories.OrderRepository;
import com.msp.repositories.RefundRepository;
import com.msp.repositories.ShiftReportRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.ShiftService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@CacheConfig(cacheNames = "shifts")
public class ShiftReportServiceImpl implements ShiftService {
        private final ShiftReportRepository shiftReportRepository;
        private final UserService userService;
        private final RefundRepository refundRepository;
        private final OrderRepository orderRepository;
        private final UserRepository userRepository;

        @Override
        @Caching(put = {
                        @CachePut(key = "#result.id")
        }, evict = {
                        @CacheEvict(value = "shifts-all", allEntries = true),
                        @CacheEvict(value = "shifts-by-branch", allEntries = true),
                        @CacheEvict(value = "shifts-by-cashier", allEntries = true),
                        @CacheEvict(value = "shifts-current", allEntries = true)
        })
        public ShiftReportDto startShift() throws Exception {
                User currentUser = userService.getCurrentUser();
                LocalDateTime shiftStart = LocalDateTime.now();
                LocalDateTime startOfDay = shiftStart.withHour(0).withMinute(0).withSecond(0);
                LocalDateTime endOfDay = shiftStart.withHour(23).withMinute(59).withSecond(59);

                Optional<ShiftReport> existing = shiftReportRepository.findByCashierAndShiftStartBetween(currentUser,
                                startOfDay, endOfDay);
                if (existing.isPresent()) {
                        throw new Exception("Shift already started today!");
                }
                Branch branch = currentUser.getBranch();
                ShiftReport shiftReport = ShiftReport.builder()
                                .cashier(currentUser)
                                .shiftStart(shiftStart)
                                .branch(branch)
                                .tenantId(currentUser.getTenantId())
                                .build();

                ShiftReport savedReport = shiftReportRepository.save(shiftReport);
                return ShiftReportMapper.toDto(savedReport);
        }

        @Override
        @Caching(put = {
                        @CachePut(key = "#result.id")
        }, evict = {
                        @CacheEvict(value = "shifts-all", allEntries = true),
                        @CacheEvict(value = "shifts-by-branch", allEntries = true),
                        @CacheEvict(value = "shifts-by-cashier", allEntries = true),
                        @CacheEvict(value = "shifts-current", allEntries = true),
                        @CacheEvict(value = "shifts-by-date", allEntries = true)
        })
        public ShiftReportDto endShift(UUID shiftReportId, LocalDateTime shiftEnd) throws Exception {
                User currentUser = userService.getCurrentUser();
                ShiftReport shiftReport = shiftReportRepository
                                .findTopByCashierAndShiftEndIsNullOrderByShiftStartDesc(currentUser)
                                .orElseThrow(() -> new Exception("Shift not found"));
                shiftReport.setShiftEnd(shiftEnd);
                List<Refund> refunds = refundRepository.findByCashierIdAndCreatedAtBetween(
                                currentUser.getId(),
                                shiftReport.getShiftStart(),
                                shiftReport.getShiftEnd());
                double totalRefunds = refunds.stream()
                                .mapToDouble(refund -> refund.getAmount() != null ? refund.getAmount() : 0.0).sum();

                List<Order> orders = orderRepository.findByCashierAndCreatedAtBetween(
                                currentUser, shiftReport.getShiftStart(), shiftReport.getShiftEnd());
                double totalSales = orders.stream().mapToDouble(Order::getTotalAmount).sum();
                int totalOrders = orders.size();
                double netSales = totalSales - totalRefunds;
                shiftReport.setTotalRefunds(totalRefunds);
                shiftReport.setTotalSales(totalSales);
                shiftReport.setTotalOrders(totalOrders);
                shiftReport.setNetSale(netSales);
                shiftReport.setRecentOrders(getRecentOrders(orders));
                shiftReport.setTopSellingProducts(getTopSellingProducts(orders));
                shiftReport.setPaymentSummaries(getPaymentSummaries(orders, totalSales));
                shiftReport.setRefunds(refunds);

                ShiftReport savedReport = shiftReportRepository.save(shiftReport);
                return ShiftReportMapper.toDto(savedReport);
        }

        @Override
        @Cacheable(key = "#id")
        public ShiftReportDto getShiftReportById(UUID id) throws Exception {
                return shiftReportRepository.findById(id)
                                .map(ShiftReportMapper::toDto)
                                .orElseThrow(
                                                () -> new Exception("Shift not found"));
        }

        @Override
        @Cacheable(value = "shifts-all", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
        public Page<ShiftReportDto> getAllShiftReports(Pageable pageable) {
                return shiftReportRepository.findAll(pageable).map(
                                ShiftReportMapper::toDto);
        }

        @Override
        @Cacheable(value = "shifts-by-branch", key = "#branchId")
        public List<ShiftReportDto> getShiftReportByBranchId(UUID branchId) {
                List<ShiftReport> reports = shiftReportRepository.findByBranchId(branchId);
                return reports.stream().map(
                                ShiftReportMapper::toDto).collect(Collectors.toList());
        }

        @Override
        @Cacheable(value = "shifts-by-cashier", key = "#cashierId")
        public List<ShiftReportDto> getShiftReportByCashierId(UUID cashierId) {
                List<ShiftReport> reports = shiftReportRepository.findByCashierId(cashierId);
                return reports.stream().map(
                                ShiftReportMapper::toDto).collect(Collectors.toList());
        }

        @Override
        @Cacheable(value = "shifts-current", key = "#cashierId != null ? #cashierId : 'current'")
        public ShiftReportDto getCurrentShiftProgress(UUID cashierId) throws Exception {
                User currentUser = userService.getCurrentUser();
                ShiftReport shift = shiftReportRepository
                                .findTopByCashierAndShiftEndIsNullOrderByShiftStartDesc(currentUser)
                                .orElseThrow(() -> new Exception("No Active Shift Found for Cashier!"));
                LocalDateTime now = LocalDateTime.now();

                List<Order> orders = orderRepository.findByCashierAndCreatedAtBetween(
                                currentUser, shift.getShiftStart(), now);
                List<Refund> refunds = refundRepository.findByCashierIdAndCreatedAtBetween(
                                currentUser.getId(),
                                shift.getShiftStart(),
                                now);
                double totalRefunds = refunds.stream()
                                .mapToDouble(refund -> refund.getAmount() != null ? refund.getAmount() : 0.0).sum();
                System.out.println("total refunds: " + totalRefunds);

                double totalSales = orders.stream()
                                .mapToDouble(Order::getTotalAmount).sum();
                int totalOrders = orders.size();

                double netSales = totalSales - totalRefunds;
                shift.setTotalRefunds(totalRefunds);
                shift.setTotalSales(totalSales);
                shift.setTotalOrders(totalOrders);
                shift.setNetSale(netSales);
                shift.setRecentOrders(getRecentOrders(orders));
                shift.setTopSellingProducts(getTopSellingProducts(orders));
                shift.setPaymentSummaries(getPaymentSummaries(orders, totalSales));
                shift.setRefunds(refunds);
                ShiftReport savedReport = shiftReportRepository.save(shift);
                return ShiftReportMapper.toDto(savedReport);
        }

        @Override
        @Cacheable(value = "shifts-by-date", key = "#cashierId + '-' + #date")
        public ShiftReportDto getShiftByCashierAndDate(UUID cashierId, LocalDateTime date) throws Exception {
                User cashier = userRepository.findById(cashierId).orElseThrow(
                                () -> new Exception("Cashier Not Found with given Id: " + cashierId));

                LocalDateTime start = date.withHour(0).withMinute(0).withSecond(0);
                LocalDateTime end = date.withHour(23).withMinute(59).withSecond(59);
                ShiftReport report = shiftReportRepository.findByCashierAndShiftStartBetween(
                                cashier, start, end)
                                .orElseThrow(() -> new Exception("Shift Report not found for Cashier!"));
                return ShiftReportMapper.toDto(report);
        }

        // helpers (no caching needed for private methods)
        private List<PaymentSummary> getPaymentSummaries(List<Order> orders, double totalSales) {
                Map<EPaymentType, List<Order>> grouped = orders.stream()
                                .collect(Collectors.groupingBy(
                                                order -> order.getPaymentType() != null ? order.getPaymentType()
                                                                : EPaymentType.CASH));
                List<PaymentSummary> summaries = new ArrayList<>();
                for (Map.Entry<EPaymentType, List<Order>> entry : grouped.entrySet()) {
                        double amount = entry.getValue().stream()
                                        .mapToDouble(Order::getTotalAmount).sum();
                        int transactions = entry.getValue().size();
                        double percent = (amount / totalSales) * 100;

                        PaymentSummary ps = new PaymentSummary();
                        ps.setType(entry.getKey());
                        ps.setTotalAmount(amount);
                        ps.setTransactionCount(transactions);
                        ps.setPercentage(percent);
                        summaries.add(ps);
                }
                return summaries;
        }

        private List<Product> getTopSellingProducts(List<Order> orders) {
                Map<Product, Integer> productSalesMap = new HashMap<>();
                for (Order order : orders) {
                        for (OrderItem item : order.getItems()) {
                                Product product = item.getProduct();
                                productSalesMap.put(product,
                                                productSalesMap.getOrDefault(product, 0) + item.getQuantity());
                        }
                }
                return productSalesMap.entrySet().stream()
                                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                                .limit(5)
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toList());
        }

        private List<Order> getRecentOrders(List<Order> orders) {
                return orders.stream()
                                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                                .limit(5)
                                .collect(Collectors.toList());
        }
}