package com.msp.repositories;

import com.msp.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<CartItem> findByUserIdAndProductId(UUID userId, UUID productId);
    void deleteByUserId(UUID userId);
    void deleteByUserIdAndProductId(UUID userId, UUID productId);
}
