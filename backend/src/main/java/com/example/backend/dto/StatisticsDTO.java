package com.example.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class StatisticsDTO {
    private long userCount;
    private long productCount;
    private long onSaleProductCount;
    private long pendingProductCount;
    private long orderCount;
    private long completedOrderCount;
    private BigDecimal totalAmount;
    private List<Map<String, Object>> orderTrend;
    private List<Map<String, Object>> categoryDistribution;
}
