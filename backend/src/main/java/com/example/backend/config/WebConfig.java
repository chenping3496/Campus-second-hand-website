package com.example.backend.config;

import com.example.backend.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类，实现WebMvcConfigurer接口，用于配置Spring MVC的各种组件
 * 包括跨域配置、拦截器配置和资源处理器配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 自动注入认证拦截器
     * 用于处理需要权限验证的请求
     */
    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 配置跨域资源共享(CORS)规则
     * 设置允许的源、方法、头部信息等
     * @paramregistry CORS注册器，用于添加跨域映射规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 匹配所有路径
                .allowedOriginPatterns("*")  // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的HTTP方法
                .allowedHeaders("*")  // 允许所有请求头
                .allowCredentials(true)  // 允许发送凭证信息
                .maxAge(3600);  // 预检请求的有效期为3600秒
    }

    /**
     * 配置拦截器
     * 添加认证拦截器，并配置拦截和排除的路径
     * @param registry 拦截器注册器，用于添加拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)  // 添加认证拦截器
                .addPathPatterns("/api/**")  // 拦截所有API路径
                .excludePathPatterns(  // 排除以下路径，不需要认证
                        "/api/auth/login",  // 登录接口
                        "/api/auth/register",  // 注册接口
                        "/api/products/list",  // 产品列表接口
                        "/api/products/detail/**",  // 产品详情接口
                        "/api/products/search",  // 产品搜索接口
                        "/api/products/tags/**",  // 产品标签接口
                        "/api/categories/list");  // 分类列表接口
    }

    /**
     * 配置静态资源处理器
     * 配置图片等静态资源的访问路径
     * @param registry 资源处理器注册器，用于添加资源处理规则
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve images from both classpath and file system (for runtime uploads)
        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        "classpath:/static/images/",
                        "file:src/main/resources/static/images/");

        // Also serve /uploads directly for compatibility
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        "classpath:/static/images/uploads/",
                        "file:src/main/resources/static/images/uploads/");
    }
}
