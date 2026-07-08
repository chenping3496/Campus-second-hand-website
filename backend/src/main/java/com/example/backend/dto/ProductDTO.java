package com.example.backend.dto;

import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private String status;
    private String rejectReason;
    private List<String> images;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private Boolean isFavorited;
    private Long favoriteCount;

    public static ProductDTO fromEntity(Product product, Category category, User seller) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setOriginalPrice(product.getOriginalPrice());
        if (category != null) {
            dto.setCategoryId(category.getId());
            dto.setCategoryName(category.getName());
        } else {
            dto.setCategoryId(product.getCategoryId());
        }
        if (seller != null) {
            dto.setSellerId(seller.getId());
            dto.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());
            dto.setSellerAvatar(seller.getAvatar());
        } else {
            dto.setSellerId(product.getSellerId());
        }
        dto.setStatus(product.getStatus().name());
        dto.setRejectReason(product.getRejectReason());
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            dto.setImages(Arrays.asList(product.getImages().split(",")));
        }
        dto.setViewCount(product.getViewCount());
        dto.setCreatedAt(product.getCreatedAt());
        return dto;
    }
}
