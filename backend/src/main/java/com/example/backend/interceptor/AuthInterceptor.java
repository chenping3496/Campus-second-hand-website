package com.example.backend.interceptor;

import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);

            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                User user = userMapper.selectById(userId);

                if (user != null) {
                    if (user.getStatus() == User.UserStatus.BANNED) {
                        sendError(response, 403, "账号已被封禁");
                        return false;
                    }

                    // Verification check for trading-related endpoints
                    String path = request.getRequestURI();
                    if (requiresVerification(path)) {
                        User.VerificationStatus vs = user.getVerificationStatus();
                        if (vs == User.VerificationStatus.UNVERIFIED) {
                            sendError(response, 403, "请先完成身份认证");
                            return false;
                        }
                        if (vs == User.VerificationStatus.PENDING) {
                            sendError(response, 403, "身份认证审核中，请耐心等待");
                            return false;
                        }
                        if (vs == User.VerificationStatus.REJECTED) {
                            sendError(response, 403, "身份认证未通过，请提交申诉");
                            return false;
                        }
                        if (vs == User.VerificationStatus.FROZEN) {
                            sendError(response, 403, "账号认证已被冻结，请联系管理员");
                            return false;
                        }
                    }

                    request.setAttribute("currentUser", user);
                    request.setAttribute("userId", userId);
                    return true;
                }
            }
        }

        sendError(response, 401, "未登录或登录已过期");
        return false;
    }

    private void sendError(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }

    /**
     * Trading-related endpoints require approved verification
     */
    private boolean requiresVerification(String path) {
        // These public endpoints do NOT require verification
        if (path.startsWith("/api/auth/")) return false;
        if (path.startsWith("/api/products/list")) return false;
        if (path.startsWith("/api/products/detail/")) return false;
        if (path.startsWith("/api/products/search")) return false;
        if (path.startsWith("/api/categories/")) return false;
        if (path.startsWith("/api/verification/")) return false;
        if (path.startsWith("/api/files/")) return false;
        if (path.startsWith("/api/user/")) return false;
        // Admin endpoints have their own check
        if (path.startsWith("/api/admin/")) return false;
        // All other endpoints require verification (POST/PUT/DELETE /api/products, /api/orders, /api/favorites, /api/chat)
        return true;
    }
}
