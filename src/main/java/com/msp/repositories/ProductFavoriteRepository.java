package com.msp.repositories;

import com.msp.models.ProductFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, UUID> {
    Page<ProductFavorite> findByUserId(UUID userId, Pageable pageable);
    Optional<ProductFavorite> findByUserIdAndProductId(UUID userId, UUID productId);
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
    void deleteByUserIdAndProductId(UUID userId, UUID productId);

    @Query("select f.product.id from ProductFavorite f where f.user.id = :userId")
    List<UUID> findProductIdsByUserId(@Param("userId") UUID userId);
}
