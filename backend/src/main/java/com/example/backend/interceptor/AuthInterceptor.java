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
}
