package com.msp.controllers;

import com.msp.mappers.ProductMapper;
import com.msp.models.Inventory;
import com.msp.models.Product;
import com.msp.payloads.dtos.BranchDto;
import com.msp.payloads.dtos.CategoryDto;
import com.msp.payloads.dtos.ProductDto;
import com.msp.payloads.dtos.StoreDto;
import com.msp.mappers.BranchMapper;
import com.msp.mappers.CategoryMapper;
import com.msp.mappers.StoreMapper;
import com.msp.repositories.BranchRepository;
import com.msp.repositories.CategoryRepository;
import com.msp.repositories.InventoryRepository;
import com.msp.repositories.ProductRepository;
import com.msp.repositories.StoreRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final ProductRepository productRepo;
    private final StoreRepository storeRepo;
    private final BranchRepository branchRepo;
    private final CategoryRepository categoryRepo;
    private final InventoryRepository inventoryRepo;

    @GetMapping("/products")
    public ResponseEntity<Page<ProductDto>> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        String sortField = switch (sortBy) {
            case "name", "sellingPrice", "mrp", "brand", "createdAt", "sku" -> sortBy;
            default -> "createdAt";
        };
        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Specification<Product> spec = (root, query, cb) -> {
            if (query != null && query.getResultType() != null && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                query.distinct(true);
                root.fetch("store", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("brand"), "")), like),
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like)
                ));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.toLowerCase()));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sellingPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sellingPrice"), maxPrice));
            }
            if (Boolean.TRUE.equals(inStock) || branchId != null) {
                Subquery<Long> sq = query.subquery(Long.class);
                Root<Inventory> inv = sq.from(Inventory.class);
                List<Predicate> invPred = new ArrayList<>();
                invPred.add(cb.equal(inv.get("product"), root));
                if (branchId != null) {
                    invPred.add(cb.equal(inv.get("branch").get("id"), branchId));
                }
                sq.select(cb.sum(inv.get("quantity"))).where(invPred.toArray(Predicate[]::new));
                predicates.add(cb.greaterThan(cb.coalesce(sq, 0L), 0L));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<ProductDto> result = productRepo.findAll(spec, PageRequest.of(page, size, Sort.by(dir, sortField)))
                .map(p -> {
                    ProductDto dto = ProductMapper.toDto(p);
                    List<Inventory> stock = inventoryRepo.findByProductId(p.getId());
                    long qty = stock.stream().mapToLong(i -> i.getQuantity() == null ? 0 : i.getQuantity()).sum();
                    dto.setStockQuantity(qty);
                    return dto;
                });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stores")
    public ResponseEntity<Page<StoreDto>> stores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(storeRepo.findAll(PageRequest.of(page, size)).map(StoreMapper::toDto));
    }

    @GetMapping("/branches")
    public ResponseEntity<Page<BranchDto>> branches(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (storeId != null) {
            return ResponseEntity.ok(branchRepo.findByStoreId(storeId, PageRequest.of(page, size)).map(BranchMapper::toDto));
        }
        return ResponseEntity.ok(branchRepo.findAll(PageRequest.of(page, size)).map(BranchMapper::toDto));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> categories(@RequestParam(required = false) UUID storeId) {
        List<CategoryDto> list = (storeId != null
                ? categoryRepo.findByStoreId(storeId, PageRequest.of(0, 200)).getContent()
                : categoryRepo.findAll())
                .stream().map(CategoryMapper::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> brands() {
        return ResponseEntity.ok(
                productRepo.findAll().stream()
                        .map(Product::getBrand)
                        .filter(b -> b != null && !b.isBlank())
                        .distinct()
                        .sorted()
                        .toList()
        );
    }
}
