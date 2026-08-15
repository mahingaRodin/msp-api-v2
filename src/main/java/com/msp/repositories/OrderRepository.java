package com.msp.repositories;

import com.msp.models.Order;
import com.msp.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByCustomerId(UUID customerId,Pageable pageable);
    Page<Order> findByBranchId(UUID branchId, Pageable pageable);
    Page<Order> findByCashier_Id(UUID cashierId,Pageable pageable);
    Page<Order> findByBranchIdAndCreatedAtBetween(UUID branchId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    List<Order> findByCashierAndCreatedAtBetween(User cashier, LocalDateTime from, LocalDateTime to);
    Page<Order> findTop5ByBranchIdOrderByCreatedAtDesc(UUID branchId,Pageable pageable);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.status <> com.msp.enums.EOrderStatus.CANCELLED")
    Double sumPlatformRevenue();

    @Query("select count(o) from Order o where o.status <> com.msp.enums.EOrderStatus.CANCELLED")
    long countPlatformOrders();

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.branch.id = :branchId and o.status <> com.msp.enums.EOrderStatus.CANCELLED")
    Double sumBranchRevenue(@Param("branchId") UUID branchId);

    @Query("select count(o) from Order o where o.branch.id = :branchId and o.status <> com.msp.enums.EOrderStatus.CANCELLED")
    long countBranchOrders(@Param("branchId") UUID branchId);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.branch.store.id = :storeId and o.status <> com.msp.enums.EOrderStatus.CANCELLED")
    Double sumStoreRevenue(@Param("storeId") UUID storeId);

    @Query("select count(o) from Order o where o.branch.store.id = :storeId and o.status <> com.msp.enums.EOrderStatus.CANCELLED")
    long countStoreOrders(@Param("storeId") UUID storeId);

    @Query("""
            select cast(o.createdAt as date), coalesce(sum(o.totalAmount), 0), count(o)
            from Order o
            where o.status <> com.msp.enums.EOrderStatus.CANCELLED and o.createdAt >= :from
            group by cast(o.createdAt as date)
            order by 1
            """)
    List<Object[]> dailyPlatform(@Param("from") LocalDateTime from);

    @Query("""
            select cast(o.createdAt as date), coalesce(sum(o.totalAmount), 0), count(o)
            from Order o
            where o.branch.id = :branchId and o.status <> com.msp.enums.EOrderStatus.CANCELLED and o.createdAt >= :from
            group by cast(o.createdAt as date)
            order by 1
            """)
    List<Object[]> dailyBranch(@Param("branchId") UUID branchId, @Param("from") LocalDateTime from);

    @Query("""
            select cast(o.createdAt as date), coalesce(sum(o.totalAmount), 0), count(o)
            from Order o
            where o.branch.store.id = :storeId and o.status <> com.msp.enums.EOrderStatus.CANCELLED and o.createdAt >= :from
            group by cast(o.createdAt as date)
            order by 1
            """)
    List<Object[]> dailyStore(@Param("storeId") UUID storeId, @Param("from") LocalDateTime from);

    @Query("""
            select o.branch.name, coalesce(sum(o.totalAmount), 0)
            from Order o
            where o.status <> com.msp.enums.EOrderStatus.CANCELLED
            group by o.branch.name
            order by 2 desc
            """)
    List<Object[]> revenueByBranch();

    @Query("""
            select o.branch.name, coalesce(sum(o.totalAmount), 0)
            from Order o
            where o.branch.store.id = :storeId and o.status <> com.msp.enums.EOrderStatus.CANCELLED
            group by o.branch.name
            order by 2 desc
            """)
    List<Object[]> revenueByBranchForStore(@Param("storeId") UUID storeId);

    @Query("""
            select oi.product.name, coalesce(sum(oi.quantity), 0)
            from OrderItem oi
            where oi.order.status <> com.msp.enums.EOrderStatus.CANCELLED
            group by oi.product.name
            order by 2 desc
            """)
    List<Object[]> topProducts();

    @Query("""
            select oi.product.name, coalesce(sum(oi.quantity), 0)
            from OrderItem oi
            where oi.order.branch.id = :branchId and oi.order.status <> com.msp.enums.EOrderStatus.CANCELLED
            group by oi.product.name
            order by 2 desc
            """)
    List<Object[]> topProductsByBranch(@Param("branchId") UUID branchId);

    @Query("""
            select oi.product.name, coalesce(sum(oi.quantity), 0)
            from OrderItem oi
            where oi.order.branch.store.id = :storeId and oi.order.status <> com.msp.enums.EOrderStatus.CANCELLED
            group by oi.product.name
            order by 2 desc
            """)
    List<Object[]> topProductsByStore(@Param("storeId") UUID storeId);
}
