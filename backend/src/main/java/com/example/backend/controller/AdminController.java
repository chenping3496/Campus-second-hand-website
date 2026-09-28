package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.entity.User;
import com.example.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理员", description = "管理员后台管理接口")
@SecurityRequirement(name = "Bearer")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private ComplaintService complaintService;

    private Result<Void> checkAdmin(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null || user.getRole() != User.UserRole.ADMIN) {
            return Result.error(403, "无权限访问");
        }
        return null;
    }

    @GetMapping("/users")
    @Operation(summary = "用户列表")
    public Result<PageResult<UserDTO>> getUserList(
            HttpServletRequest request,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return Result.success(userService.getUserListForAdmin(keyword, status, page, size));
    }

    @PostMapping("/users/{id}/ban")
    @Operation(summary = "封禁用户")
    public Result<Void> banUser(@PathVariable Long id, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return userService.banUser(id);
    }

    @PostMapping("/users/{id}/unban")
    @Operation(summary = "解禁用户")
    public Result<Void> unbanUser(@PathVariable Long id, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return userService.unbanUser(id);
    }

    @GetMapping("/products")
    @Operation(summary = "商品管理列表")
    public Result<PageResult<ProductDTO>> getProductList(
            HttpServletRequest request,
            @Parameter(description = "商品状态") @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return Result.success(productService.getProductListForAdmin(status, keyword, page, size));
    }

    @PostMapping("/products/{id}/approve")
    @Operation(summary = "审核通过商品")
    public Result<Void> approveProduct(@PathVariable Long id, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return productService.approveProduct(id);
    }

    @PostMapping("/products/{id}/reject")
    @Operation(summary = "拒绝商品上架")
    public Result<Void> rejectProduct(@PathVariable Long id, HttpServletRequest request,
                                      @RequestBody Map<String, String> params) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        String reason = params.get("reason");
        return productService.rejectProduct(id, reason);
    }

    @PostMapping("/products/{id}/force-off-shelf")
    @Operation(summary = "强制下架商品")
    public Result<Void> forceOffShelf(@PathVariable Long id, HttpServletRequest request,
                                      @RequestBody Map<String, String> params) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        String reason = params.get("reason");
        return productService.forceOffShelf(id, reason);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取全部分类")
    public Result<List<CategoryDTO>> getAllCategories(HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return Result.success(categoryService.getAllCategories());
    }

    @PostMapping("/categories")
    @Operation(summary = "创建分类")
    public Result<CategoryDTO> createCategory(HttpServletRequest request, @RequestBody CategoryDTO dto) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return categoryService.createCategory(dto);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "修改分类")
    public Result<CategoryDTO> updateCategory(@PathVariable Long id, HttpServletRequest request,
                                              @RequestBody CategoryDTO dto) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return categoryService.updateCategory(id, dto);
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "删除分类")
    public Result<Void> deleteCategory(@PathVariable Long id, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return categoryService.deleteCategory(id);
    }

    @GetMapping("/orders")
    @Operation(summary = "订单管理列表")
    public Result<PageResult<OrderDTO>> getOrderList(
            HttpServletRequest request,
            @Parameter(description = "订单状态") @RequestParam(required = false) String status,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return Result.success(orderService.getOrderListForAdmin(status, orderNo, page, size));
    }

    // ============ Verification management ============

    @GetMapping("/verifications/list")
    @Operation(summary = "认证申请列表")
    public Result<PageResult<VerificationDTO>> getVerificationList(
            HttpServletRequest request,
            @Parameter(description = "认证状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return Result.success(verificationService.getVerificationList(status, page, size));
    }

    @PostMapping("/verifications/{userId}/approve")
    @Operation(summary = "审核通过认证")
    public Result<Void> approveVerification(@PathVariable Long userId, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return verificationService.approve(userId);
    }

    @PostMapping("/verifications/{userId}/reject")
    @Operation(summary = "驳回认证")
    public Result<Void> rejectVerification(@PathVariable Long userId, HttpServletRequest request,
                                           @RequestBody Map<String, String> params) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return verificationService.reject(userId, params.get("reason"));
    }

    @PostMapping("/verifications/{userId}/freeze")
    @Operation(summary = "冻结用户认证")
    public Result<Void> freezeVerification(@PathVariable Long userId, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return verificationService.freeze(userId);
    }

    @PostMapping("/verifications/{userId}/unfreeze")
    @Operation(summary = "解冻用户认证")
    public Result<Void> unfreezeVerification(@PathVariable Long userId, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return verificationService.unfreeze(userId);
    }

    @PostMapping("/verifications/{userId}/review")
    @Operation(summary = "复核用户认证")
    public Result<Void> reviewVerification(@PathVariable Long userId, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        return verificationService.review(userId);
    }

    @GetMapping("/verifications/appeals")
    @Operation(summary = "申诉列表")
    public Result<PageResult<AppealDTO>> getAppeals(
            HttpServletRequest request,
            @Parameter(description = "申诉状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return Result.success(verificationService.getAppeals(status, page, size));
    }

    @PostMapping("/verifications/appeals/{id}/handle")
    @Operation(summary = "处理申诉")
    public Result<Void> handleAppeal(@PathVariable Long id, HttpServletRequest request,
                                     @RequestBody Map<String, Object> params) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        boolean approved = Boolean.TRUE.equals(params.get("approved"));
        String response = (String) params.get("response");
        return verificationService.handleAppeal(id, approved, response);
    }

    // ============ Complaint management ============

    @GetMapping("/complaints")
    @Operation(summary = "投诉列表")
    public Result<PageResult<ComplaintDTO>> getComplaintList(
            HttpServletRequest request,
            @Parameter(description = "投诉状态") @RequestParam(required = false) String status,
            @Parameter(description = "投诉类型") @RequestParam(required = false) String type,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return Result.error(check.getCode(), check.getMessage());
        return Result.success(complaintService.getComplaintList(status, type, page, size));
    }

    @PostMapping("/complaints/{id}/processing")
    @Operation(summary = "标记处理中")
    public Result<Void> markProcessing(@PathVariable Long id, HttpServletRequest request) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        User admin = (User) request.getAttribute("currentUser");
        return complaintService.markProcessing(id, admin.getId());
    }

    @PostMapping("/complaints/{id}/resolve")
    @Operation(summary = "解决投诉")
    public Result<Void> resolveComplaint(@PathVariable Long id, HttpServletRequest request,
                                         @RequestBody Map<String, String> params) {
        Result<Void> check = checkAdmin(request);
        if (check != null) return check;
        User admin = (User) request.getAttribute("currentUser");
        return complaintService.handleComplaint(id, admin.getId(), params.get("response"));
    }
}
