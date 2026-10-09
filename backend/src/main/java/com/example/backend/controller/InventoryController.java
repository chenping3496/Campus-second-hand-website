package com.example.backend.controller;

import com.example.backend.dto.Result;
import com.example.backend.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 库存控制器 - 演示第四类并发竞争问题的解决方案
 * 提供商品库存相关的API
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    /**
     * 获取商品实时库存
     */
    @GetMapping("/stock/{productId}")
    public Result<Map<String, Object>> getProductStock(@PathVariable Long productId) {
        try {
            Integer stock = inventoryService.getRealTimeStock(productId);

            Map<String, Object> data = new HashMap<>();
            data.put("productId", productId);
            data.put("stock", stock);
            data.put("timestamp", System.currentTimeMillis());

            return Result.success(data);

        } catch (Exception e) {
            return Result.error("获取库存失败: " + e.getMessage());
        }
    }

    /**
     * 安全的购买接口（使用预扣机制）
     * 解决商品状态并发更新问题
     */
    @PostMapping("/purchase")
    public Result<?> safePurchase(@RequestBody PurchaseRequest request) {
        try {
            if (request.getBuyerId() == null || request.getProductId() == null) {
                return Result.error("参数错误：买家ID和商品ID不能为空");
            }

            // 使用带预扣的订单创建方法
            return inventoryService.createOrderWithStockReserve(
                request.getBuyerId(),
                request.getProductId(),
                request.getPaymentMethod()
            );

        } catch (Exception e) {
            return Result.error("购买失败: " + e.getMessage());
        }
    }

    /**
     * 同步商品库存到Redis（管理接口）
     */
    @PostMapping("/sync/{productId}")
    public Result<String> syncStockToRedis(@PathVariable Long productId,
                                         @RequestParam(defaultValue = "1") Integer stock) {
        try {
            inventoryService.initProductStock(productId, stock);
            return Result.success("库存初始化成功: productId=" + productId + ", stock=" + stock);
        } catch (Exception e) {
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 模拟库存压力测试
     */
    @GetMapping("/stress-test/{productId}")
    public Result<Map<String, Object>> stressTest(@PathVariable Long productId) {
        try {
            int initialStock = 100;
            int concurrentRequests = 10;

            // 模拟多个并发请求同时减少库存
            Map<String, Object> result = new HashMap<>();
            result.put("productId", productId);
            result.put("initialStock", initialStock);
            result.put("concurrentRequests", concurrentRequests);
            result.put("testDescription", "模拟" + concurrentRequests + "个并发请求同时购买1件商品");
            result.put("expectedResult", "只有第一个请求能成功，其余应返回库存不足");
            result.put("timestamp", System.currentTimeMillis());

            // 注意：这是一个演示，实际并发测试需要多线程

            return Result.success(result);

        } catch (Exception e) {
            return Result.error("测试失败: " + e.getMessage());
        }
    }

    // 购买请求DTO
    public static class PurchaseRequest {
        private Long buyerId;
        private Long productId;
        private String paymentMethod;

        // Getters and setters
        public Long getBuyerId() { return buyerId; }
        public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }
}