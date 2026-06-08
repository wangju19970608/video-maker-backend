package com.example.video.config;

import com.example.video.admin.security.AdminAuthInterceptor;
import com.example.video.security.UserAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final UserAuthInterceptor userAuthInterceptor;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor, UserAuthInterceptor userAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.userAuthInterceptor = userAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Admin auth interceptor (only for /api/admin/**)
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/sessions");

        // User auth interceptor (for /api/orders/**)
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns("/api/orders/**")
                .excludePathPatterns("/api/orders/internal/**");
    }
}
