package com.example.backend.config;

import com.example.backend.entity.Product;
import com.example.backend.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动初始化任务
 * 在应用启动时初始化Redis库存数据
 */
@Component
public class InventoryInitializer implements CommandLineRunner {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(String... args) throws Exception {
        initializeProductStocks();
        System.out.println("Inventory initialization completed.");
    }

    /**
     * 初始化所有商品库存到Redis
     */
    private void initializeProductStocks() {
        try {
            // 查询所有商品
            List<Product> products = productMapper.selectList(null);
            if (products == null || products.isEmpty()) {
                System.out.println("No products found for initialization.");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (Product product : products) {
                try {
                    // 直接初始化库存到Redis
                    String stockKey = "product:stock:" + product.getId();
                    // 每个商品默认库存为1（二手物品交易）
                    stringRedisTemplate.opsForValue().set(
                        stockKey,
                        "1"
                    );
                    System.out.println("Initialized product stock to Redis: productId=" +
                        product.getId() + ", stock=1");
                    successCount++;
                } catch (Exception e) {
                    System.err.println("Failed to initialize stock for product " + product.getId() + ": " + e.getMessage());
                    failCount++;
                }
            }

            System.out.println("Product stock initialization complete: " +
                successCount + " successful, " + failCount + " failed.");

        } catch (Exception e) {
            System.err.println("Error initializing product stocks: " + e.getMessage());
        }
    }
}