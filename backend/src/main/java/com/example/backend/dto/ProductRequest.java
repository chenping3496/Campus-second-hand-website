package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {//requestdto类型用于前端申请创建，且有大量参数，常常需要验证
    @NotBlank(message = "商品标题不能为空")
    private String title;
    
    private String description;
    
    @NotNull(message = "商品价格不能为空")
    @Positive(message = "商品价格必须大于0")
    private BigDecimal price;
    
    private BigDecimal originalPrice;
    
    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    private String productTag;

    private List<String> images;
}
