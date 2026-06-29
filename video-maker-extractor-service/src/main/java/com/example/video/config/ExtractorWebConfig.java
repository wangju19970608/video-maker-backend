package com.example.video.config;

import com.example.video.security.UserAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ExtractorWebConfig implements WebMvcConfigurer {

    private final UserAuthInterceptor userAuthInterceptor;

    public ExtractorWebConfig(UserAuthInterceptor userAuthInterceptor) {
        this.userAuthInterceptor = userAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Protect Extractor video related APIs
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns("/api/extractor/video/**");
    }
}
