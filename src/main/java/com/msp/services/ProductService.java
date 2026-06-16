package com.msp.services;

import com.msp.payloads.dtos.ProductDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ProductService {
    ProductDto createProduct(ProductDto productDto) throws Exception;
    ProductDto updateProduct(UUID id, ProductDto productDto) throws Exception;
    void deleteProduct(UUID id) throws Exception;
    ProductDto getProductById(UUID id) throws Exception;
    Page<ProductDto> getProductsByStoreId(UUID storeId, int page, int size);
    Page<ProductDto> searchByKeyword(UUID storeId, String keyword, int page, int size);
}
