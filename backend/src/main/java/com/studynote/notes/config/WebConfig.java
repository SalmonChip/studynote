package com.studynote.notes.config;

import com.studynote.notes.filter.TraceIdFilter;
import com.studynote.notes.interceptor.AdminInterceptor;
import com.studynote.notes.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:D:/studyNote/upload}")
    private String uploadPath;

    @Autowired
    private TokenInterceptor tokenInterceptor;

    @Autowired
    private AdminInterceptor adminInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }

    /**
     * 添加拦截器，用于验证 token，初始化请求周期中的用户相关信息
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 顺序要紧：先解析 token 填好 RequestScopeData，再判权限
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/error");

        // 管理员接口：只拦 /api/admin/** 这一片
        // 这里就是「默认安全」的落点 —— 以后新加的任何 /api/admin/xxx 接口，
        // 不用改任何代码，自动就在保护范围内
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")  // 允许的域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")// 允许的 HTTP 方法
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceIdFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}