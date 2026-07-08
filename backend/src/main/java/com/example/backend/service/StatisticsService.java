package com.example.backend.service;

import com.example.backend.dto.Result;
import com.example.backend.dto.StatisticsDTO;
import com.example.backend.mapper.CategoryMapper;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.mapper.ProductMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatisticsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    public StatisticsDTO getStatistics() {
        StatisticsDTO dto = new StatisticsDTO();

        dto.setUserCount(userMapper.countUsers());
        dto.setProductCount(productMapper.countAllProducts());
        dto.setOnSaleProductCount(productMapper.countOnSaleProducts());
        dto.setPendingProductCount(productMapper.countPendingProducts());
        dto.setOrderCount(orderMapper.countAllOrders());
        dto.setCompletedOrderCount(orderMapper.countCompletedOrders());

        BigDecimal totalAmount = orderMapper.sumCompletedOrdersAmount();
        dto.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);

        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        List<Map<String, Object>> orderTrendData = orderMapper.countOrdersByDate(startDate);
        List<Map<String, Object>> orderTrend = new ArrayList<>();
        for (Map<String, Object> row : orderTrendData) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row.get("d").toString());
            item.put("count", row.get("cnt"));
            orderTrend.add(item);
        }
        dto.setOrderTrend(orderTrend);

        List<Map<String, Object>> categoryData = productMapper.countByCategory();
        List<Map<String, Object>> categoryDistribution = new ArrayList<>();
        for (Map<String, Object> row : categoryData) {
            Object categoryIdObj = row.get("category_id");
            Long categoryId = categoryIdObj instanceof Long ? (Long) categoryIdObj : ((Number) categoryIdObj).longValue();
            Object cntObj = row.get("cnt");
            Long count = cntObj instanceof Long ? (Long) cntObj : ((Number) cntObj).longValue();
            categoryMapper.selectById(categoryId);
            var category = categoryMapper.selectById(categoryId);
            if (category != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("categoryId", categoryId);
                item.put("categoryName", category.getName());
                item.put("count", count);
                categoryDistribution.add(item);
            }
        }
        dto.setCategoryDistribution(categoryDistribution);

        return dto;
    }
}
