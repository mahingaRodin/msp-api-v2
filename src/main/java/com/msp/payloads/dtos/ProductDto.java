package com.msp.payloads.dtos;

import com.msp.models.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private UUID id;
    private String name;
    private String sku;
    private String description;
    private Double mrp;
    private Double sellingPrice;
    private String brand;
    private String image;
    private CategoryDto category;
    private UUID categoryId;
    private UUID storeId;
    private String storeName;
    private String storeBrand;
    private String categoryName;
    private Long stockQuantity;
    private Boolean favorite;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
