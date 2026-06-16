package com.msp.services;

import com.msp.payloads.dtos.CategoryDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CategoryService {
    CategoryDto createCategory(CategoryDto categoryDto) throws Exception;
    Page<CategoryDto> getCategoriesByStore(UUID storeId, int page, int size);
    CategoryDto updateCategory(UUID id, CategoryDto categoryDto) throws Exception;
    void deleteCategory(UUID id) throws Exception;
    CategoryDto getCategoryById(UUID id) throws Exception;
}
