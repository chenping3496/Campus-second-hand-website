package com.example.backend.dto;

import com.example.backend.entity.Order;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private Long productId;
    private String productTitle;
    private String productImage;
    private Long buyerId;
    private String buyerName;
    private String buyerAvatar;
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private BigDecimal price;
    private String status;
    private String paymentMethod;
    private LocalDateTime paymentTime;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createdAt;

    public static OrderDTO fromEntity(Order order, Product product, User buyer, User seller) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductTitle(product.getTitle());
            String images = product.getImages();
            if (images != null && !images.isEmpty()) {
                dto.setProductImage(images.split(",")[0]);
            }
        } else {
            dto.setProductId(order.getProductId());
        }
        if (buyer != null) {
            dto.setBuyerId(buyer.getId());
            dto.setBuyerName(buyer.getNickname() != null ? buyer.getNickname() : buyer.getUsername());
            dto.setBuyerAvatar(buyer.getAvatar());
        } else {
            dto.setBuyerId(order.getBuyerId());
        }
        if (seller != null) {
            dto.setSellerId(seller.getId());
            dto.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());
            dto.setSellerAvatar(seller.getAvatar());
        } else {
            dto.setSellerId(order.getSellerId());
        }
        dto.setPrice(order.getPrice());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentTime(order.getPaymentTime());
        dto.setShipTime(order.getShipTime());
        dto.setCompleteTime(order.getCompleteTime());
        dto.setCancelTime(order.getCancelTime());
        dto.setCancelReason(order.getCancelReason());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}
