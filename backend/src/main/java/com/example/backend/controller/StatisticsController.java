package com.example.backend.controller;

import com.example.backend.dto.Result;
import com.example.backend.dto.StatisticsDTO;
import com.example.backend.entity.User;
import com.example.backend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "统计", description = "平台数据统计接口")
@SecurityRequirement(name = "Bearer")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    private Result<Void> checkAdmin(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null || user.getRole() != User.UserRole.ADMIN) {
            return Result.error(403, "无权限访问");
        }
        return null;
    }

    @GetMapping
    @Operation(summary = "数据统计面板")
    public Result<StatisticsDTO> getStatistics(HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) {
            return Result.error(check.getCode(), check.getMessage());
        }
        return Result.success(statisticsService.getStatistics());
    }
}
