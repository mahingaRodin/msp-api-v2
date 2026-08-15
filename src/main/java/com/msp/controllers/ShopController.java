package com.msp.controllers;

import com.msp.enums.EUserRole;
import com.msp.exceptions.UserException;
import com.msp.mappers.ProductMapper;
import com.msp.models.Branch;
import com.msp.models.CartItem;
import com.msp.models.Customer;
import com.msp.models.Product;
import com.msp.models.ProductFavorite;
import com.msp.models.User;
import com.msp.payloads.dtos.OrderDto;
import com.msp.payloads.dtos.OrderItemDto;
import com.msp.payloads.dtos.ProductDto;
import com.msp.payloads.request.CartItemRequest;
import com.msp.payloads.request.CheckoutRequest;
import com.msp.payloads.response.ApiResponse2;
import com.msp.repositories.BranchRepository;
import com.msp.repositories.CartItemRepository;
import com.msp.repositories.CustomerRepository;
import com.msp.repositories.ProductFavoriteRepository;
import com.msp.repositories.ProductRepository;
import com.msp.services.OrderService;
import com.msp.services.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class ShopController {

    private final UserService userService;
    private final ProductRepository productRepo;
    private final ProductFavoriteRepository favoriteRepo;
    private final CartItemRepository cartRepo;
    private final BranchRepository branchRepo;
    private final CustomerRepository customerRepo;
    private final OrderService orderService;

    @GetMapping("/favorites")
    public ResponseEntity<Page<ProductDto>> favorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(
                favoriteRepo.findByUserId(user.getId(), PageRequest.of(page, size))
                        .map(f -> ProductMapper.toDto(f.getProduct()))
        );
    }

    @PostMapping("/favorites/{productId}")
    @Transactional
    public ResponseEntity<ApiResponse2> addFavorite(@PathVariable UUID productId) {
        User user = userService.getCurrentUser();
        Product product = productRepo.findById(productId).orElseThrow(() -> new UserException("Product not found"));
        if (!favoriteRepo.existsByUserIdAndProductId(user.getId(), productId)) {
            favoriteRepo.save(ProductFavorite.builder()
                    .user(user)
                    .product(product)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        ApiResponse2 res = new ApiResponse2();
        res.setMessage("Saved to favorites");
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/favorites/{productId}")
    @Transactional
    public ResponseEntity<ApiResponse2> removeFavorite(@PathVariable UUID productId) {
        User user = userService.getCurrentUser();
        favoriteRepo.deleteByUserIdAndProductId(user.getId(), productId);
        ApiResponse2 res = new ApiResponse2();
        res.setMessage("Removed from favorites");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/cart")
    public ResponseEntity<List<CartLine>> cart() {
        User user = userService.getCurrentUser();
        List<CartLine> lines = cartRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(item -> {
            CartLine line = new CartLine();
            line.setId(item.getId());
            line.setQuantity(item.getQuantity());
            line.setBranchId(item.getBranch() != null ? item.getBranch().getId() : null);
            line.setProduct(ProductMapper.toDto(item.getProduct()));
            return line;
        }).toList();
        return ResponseEntity.ok(lines);
    }

    @PostMapping("/cart")
    @Transactional
    public ResponseEntity<ApiResponse2> addToCart(@RequestBody CartItemRequest request) {
        User user = userService.getCurrentUser();
        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new UserException("Product not found"));
        CartItem item = cartRepo.findByUserIdAndProductId(user.getId(), request.getProductId())
                .orElseGet(() -> CartItem.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .createdAt(LocalDateTime.now())
                        .build());
        item.setQuantity(item.getQuantity() + Math.max(1, request.getQuantity()));
        if (request.getBranchId() != null) {
            Branch branch = branchRepo.findById(request.getBranchId())
                    .orElseThrow(() -> new UserException("Branch not found"));
            item.setBranch(branch);
        }
        item.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(item);
        ApiResponse2 res = new ApiResponse2();
        res.setMessage("Added to cart");
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/cart/{productId}")
    @Transactional
    public ResponseEntity<ApiResponse2> removeCart(@PathVariable UUID productId) {
        User user = userService.getCurrentUser();
        cartRepo.deleteByUserIdAndProductId(user.getId(), productId);
        ApiResponse2 res = new ApiResponse2();
        res.setMessage("Removed from cart");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<OrderDto> checkout(@RequestBody CheckoutRequest request) throws Exception {
        User user = userService.getCurrentUser();
        if (user.getRole() != EUserRole.ROLE_CUSTOMER) {
            throw new UserException("Only customers can checkout from the shop");
        }
        List<CartItem> items = cartRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (items.isEmpty()) {
            throw new UserException("Your cart is empty");
        }
        Customer customer = customerRepo.findByEmail(user.getEmail())
                .orElseThrow(() -> new UserException("Customer profile missing. Contact support."));

        OrderDto dto = new OrderDto();
        dto.setBranchId(request.getBranchId());
        dto.setCustomerId(customer.getId());
        dto.setPaymentType(request.getPaymentType());
        dto.setItems(items.stream().map(ci -> {
            OrderItemDto line = new OrderItemDto();
            line.setProductId(ci.getProduct().getId());
            line.setQuantity(ci.getQuantity());
            line.setPrice(ci.getProduct().getSellingPrice());
            return line;
        }).toList());

        OrderDto saved = orderService.createOrder(dto);
        cartRepo.deleteByUserId(user.getId());
        return ResponseEntity.ok(saved);
    }

    @Data
    public static class CartLine {
        private UUID id;
        private int quantity;
        private UUID branchId;
        private ProductDto product;
    }
}
