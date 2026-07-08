package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Order selectByOrderNo(String orderNo);

    @Select("SELECT DATE(created_at) as d, COUNT(*) as cnt FROM orders WHERE created_at >= #{startDate} GROUP BY DATE(created_at) ORDER BY d")
    List<Map<String, Object>> countOrdersByDate(LocalDateTime startDate);

    @Select("SELECT COUNT(*) FROM orders")
    long countAllOrders();

    @Select("SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED'")
    long countCompletedOrders();

    @Select("SELECT COALESCE(SUM(price), 0) FROM orders WHERE status = 'COMPLETED'")
    BigDecimal sumCompletedOrdersAmount();
}
