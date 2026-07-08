package com.example.backend.dto;

import com.example.backend.entity.Category;
import lombok.Data;
import java.time.LocalDateTime;

@Data    //用于自动生成get set等方法
public class CategoryDTO {
    private Long id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    
    public static CategoryDTO fromEntity(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setSortOrder(category.getSortOrder());
        dto.setEnabled(category.getEnabled());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }
}
