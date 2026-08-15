package com.msp.mappers;

import com.msp.models.Category;
import com.msp.models.Product;
import com.msp.models.Store;
import com.msp.payloads.dtos.ProductDto;

public class ProductMapper {
    public static ProductDto toDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .mrp(product.getMrp())
                .sellingPrice(product.getSellingPrice())
                .brand(product.getBrand())
                .category(CategoryMapper.toDto(product.getCategory()))
                .storeId(product.getStore() != null ? product.getStore().getId() : null)
                .storeName(product.getStore() != null ? product.getStore().getBrand() : null)
                .storeBrand(product.getStore() != null ? product.getStore().getBrand() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .image(product.getImage())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static Product toEntity(ProductDto productDto, Store store, Category category) {
        return Product.builder()
                .name(productDto.getName())
                .store(store)
                .category(category)
                .sku(productDto.getSku())
                .description(productDto.getDescription())
                .mrp(productDto.getMrp())
                .sellingPrice(productDto.getSellingPrice())
                .brand(productDto.getBrand())
                .image(productDto.getImage())
                .build();
    }
}
