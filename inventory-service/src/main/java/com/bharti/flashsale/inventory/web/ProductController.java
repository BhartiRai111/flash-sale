package com.bharti.flashsale.inventory.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bharti.flashsale.inventory.service.ProductService;
import com.bharti.flashsale.inventory.web.ProductDtos.CreateProductRequest;
import com.bharti.flashsale.inventory.web.ProductDtos.ProductResponse;
import com.bharti.flashsale.inventory.web.ProductDtos.RestockRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> list() {
        return productService.getProducts();
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable Long productId) {
        return productService.getProduct(productId);
    }

    /** ADMIN only. Roles come from the JWT, forwarded by the gateway in X-User-Roles. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@RequestHeader(value = "X-User-Roles", required = false) String roles,
                                  @Valid @RequestBody CreateProductRequest request) {
        requireAdmin(roles);
        return productService.create(request);
    }

    @PutMapping("/{productId}/stock")
    public ProductResponse restock(@RequestHeader(value = "X-User-Roles", required = false) String roles,
                                   @PathVariable Long productId,
                                   @Valid @RequestBody RestockRequest request) {
        requireAdmin(roles);
        return productService.restock(productId, request.quantity());
    }

    private void requireAdmin(String roles) {
        boolean isAdmin = roles != null && Arrays.asList(roles.split(",")).contains("ADMIN");
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role required");
        }
    }
}
