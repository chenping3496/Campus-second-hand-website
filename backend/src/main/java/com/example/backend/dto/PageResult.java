package com.example.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {//统一分页相应格式
    private List<T> list;
    private long total;
    private int page;
    private int size;
    private int totalPages;
    
    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        return result;
    }
}
