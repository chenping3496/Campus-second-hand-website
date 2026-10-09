package com.example.backend.service;

import com.example.backend.dto.Result;
import com.example.backend.entity.Product;
import com.example.backend.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 库存服务 - 处理商品库存的并发控制
 * 基于Redis实现原子预扣，解决商品状态并发更新问题
 * 假设每个二手商品库存为1（二手物品交易场景）
 */
@Service
public class InventoryService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<Long> productReserveScript;

    @Autowired
    private DefaultRedisScript<Long> productRollbackScript;

    @Autowired
    private DefaultRedisScript<Long> productConfirmScript;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderService orderService;

    // 库存键前缀
    private static final String STOCK_KEY_PREFIX = "product:stock:";
    // 预扣键前缀
    private static final String RESERVE_KEY_PREFIX = "product:reserve:";
    // 默认预扣超时时间（秒）
    private static final int DEFAULT_RESERVE_TIMEOUT = 300; // 5分钟

    /**
     * 检查商品是否存在和状态
     */
    private boolean isProductAvailable(Long productId) {
        Product product = productMapper.selectById(productId);
        return product != null && product.getStatus() == Product.ProductStatus.ON_SALE;
    }

    /**
     * 带预扣库存的创建订单方法
     * 解决商品状态并发更新问题
     */
    @Transactional
    public Result<?> createOrderWithStockReserve(Long buyerId, Long productId, String paymentMethod) {
        // 检查商品状态
        if (!isProductAvailable(productId)) {
            return Result.error("商品不存在或不在售");
        }

        // 生成预扣ID
        String reserveId = generateReserveId();

        // 1. Redis预扣库存（原子操作）
        int reserveResult = reserveProductStock(productId, 1, reserveId);

        if (reserveResult == 0) {
            return Result.error("商品已被购买，库存不足");
        } else if (reserveResult == -1) {
            // 幂等处理：预扣已存在，可能订单已创建
            System.out.println("幂等：预扣已存在，reserveId=" + reserveId);
            // 可以查询是否有相关订单，这里简化返回
            return Result.error("正在处理购买请求，请稍后查看订单状态");
        }

        try {
            // 2. 创建订单（使用原始的OrderService）
            Result<?> orderResult = orderService.createOrder(buyerId, productId, paymentMethod);

            if (orderResult.getCode() == 200) {
                // 3. 确认预扣（支付成功）
                int confirmResult = confirmProductReserve(productId, reserveId);
                if (confirmResult != 1) {
                    System.err.println("预扣确认失败，但订单已创建成功 productId=" + productId);
                }

                // 4. 更新商品状态为已售出
                updateProductStatus(productId, Product.ProductStatus.SOLD);

                return orderResult;

            } else {
                // 订单创建失败，回滚预扣
                rollbackProductReserve(productId, reserveId);
                return orderResult;
            }

        } catch (Exception e) {
            // 异常情况，回滚预扣
            rollbackProductReserve(productId, reserveId);
            throw e;
        }
    }

    /**
     * 原子预扣库存
     */
    private int reserveProductStock(Long productId, Integer quantity, String reserveId) {
        List<String> keys = Arrays.asList(
            getStockKey(productId),
            getReserveKey(productId, reserveId)
        );

        try {
            Long result = stringRedisTemplate.execute(productReserveScript, keys,
                quantity.toString(),
                String.valueOf(DEFAULT_RESERVE_TIMEOUT),
                reserveId
            );
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            System.err.println("预扣库存失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 确认预扣（支付成功后调用）
     */
    private int confirmProductReserve(Long productId, String reserveId) {
        List<String> keys = Arrays.asList(
            getStockKey(productId),
            getReserveKey(productId, reserveId)
        );

        try {
            Long result = stringRedisTemplate.execute(productConfirmScript, keys, reserveId);
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            System.err.println("确认预扣失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 回滚预扣（支付失败或取消订单时调用）
     */
    private int rollbackProductReserve(Long productId, String reserveId) {
        List<String> keys = Arrays.asList(
            getStockKey(productId),
            getReserveKey(productId, reserveId)
        );

        try {
            Long result = stringRedisTemplate.execute(productRollbackScript, keys, reserveId);
            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            System.err.println("回滚预扣失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 更新商品状态
     */
    private void updateProductStatus(Long productId, Product.ProductStatus status) {
        try {
            Product product = productMapper.selectById(productId);
            if (product != null) {
                product.setStatus(status);
                productMapper.updateById(product);
                System.out.println("Updated product status: productId=" + productId + ", status=" + status);
            }
        } catch (Exception e) {
            System.err.println("更新商品状态失败: " + e.getMessage());
        }
    }

    /**
     * 取消订单时的库存处理
     */
    public Result<Void> handleOrderCancellation(Long orderId, Long productId) {
        // 实际项目中，orderId应该关联到reserveId
        // 这里简化处理：直接在Redis增加库存
        try {
            stringRedisTemplate.opsForValue().increment(getStockKey(productId), 1);

            // 更新商品状态为在售
            updateProductStatus(productId, Product.ProductStatus.ON_SALE);

            return Result.success();
        } catch (Exception e) {
            return Result.error("取消订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取商品实时库存
     */
    public Integer getRealTimeStock(Long productId) {
        try {
            String stockStr = stringRedisTemplate.opsForValue().get(getStockKey(productId));
            if (stockStr != null) {
                return Integer.parseInt(stockStr);
            }
            return null;
        } catch (Exception e) {
            System.err.println("获取实时库存失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查商品是否可购买（库存>0）
     */
    public boolean isProductPurchasable(Long productId) {
        Integer stock = getRealTimeStock(productId);
        return stock != null && stock > 0 && isProductAvailable(productId);
    }

    /**
     * 初始化商品库存（每个商品默认库存为1）
     */
    public void initProductStock(Long productId, Integer stock) {
        if (stock == null || stock < 0) {
            stock = 1; // 默认每个二手商品库存为1
        }
        stringRedisTemplate.opsForValue().set(getStockKey(productId), stock.toString());
    }

    /**
     * 重置商品库存（用于测试）
     */
    public void resetProductStock(Long productId, Integer stock) {
        if (stock == null || stock < 0) {
            stock = 1;
        }
        stringRedisTemplate.opsForValue().set(getStockKey(productId), stock.toString());
        if (stock > 0) {
            updateProductStatus(productId, Product.ProductStatus.ON_SALE);
        }
    }

    // ============ 工具方法 ============

    private String getStockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    private String getReserveKey(Long productId, String reserveId) {
        return RESERVE_KEY_PREFIX + productId + ":" + reserveId;
    }

    private String generateReserveId() {
        return "reserve-" + System.currentTimeMillis() + "-" +
            UUID.randomUUID().toString().substring(0, 8);
    }
}