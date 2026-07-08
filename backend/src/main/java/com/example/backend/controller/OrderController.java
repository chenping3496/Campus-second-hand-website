package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单", description = "订单创建与管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    @Operation(summary = "创建订单")
    public Result<OrderDTO> createOrder(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        Long userId = (Long) request.getAttribute("userId");
        Long productId = Long.valueOf(params.get("productId").toString());
        String paymentMethod = (String) params.get("paymentMethod");
        return orderService.createOrder(userId, productId, paymentMethod);
    }

    @GetMapping("/bought")
    @Operation(summary = "我买到的订单")
    public Result<PageResult<OrderDTO>> getBuyerOrders(
            HttpServletRequest request,
            @Parameter(description = "订单状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.getBuyerOrders(userId, status, page, size));
    }

    @GetMapping("/sold")
    @Operation(summary = "我卖出的订单")
    public Result<PageResult<OrderDTO>> getSellerOrders(
            HttpServletRequest request,
            @Parameter(description = "订单状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.getSellerOrders(userId, status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取订单详情")
    public Result<OrderDTO> getOrderDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return orderService.getOrderDetail(id, userId);
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "卖家发货")
    public Result<Void> shipOrder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return orderService.shipOrder(id, userId);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "买家确认收货")
    public Result<Void> confirmReceive(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return orderService.confirmReceive(id, userId);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result<Void> cancelOrder(@PathVariable Long id, HttpServletRequest request,
                                    @RequestBody(required = false) Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        String reason = params != null ? params.get("reason") : null;
        return orderService.cancelOrder(id, userId, reason);
    }
}
