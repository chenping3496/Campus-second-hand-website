package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "投诉", description = "用户投诉相关接口")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @PostMapping
    @Operation(summary = "提交投诉")
    public Result<Void> submitComplaint(HttpServletRequest request,
                                        @Valid @RequestBody ComplaintRequest complaintRequest) {
        Long userId = (Long) request.getAttribute("userId");
        return complaintService.submitComplaint(userId, complaintRequest);
    }

    @GetMapping("/my")
    @Operation(summary = "我的投诉列表")
    public Result<PageResult<ComplaintDTO>> getMyComplaints(
            HttpServletRequest request,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(complaintService.getMyComplaints(userId, page, size));
    }
}
