package com.studynote.notes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .formLogin().disable()
            .httpBasic().disable();
        
        return http.build();
    }

    /**
     * 跨域配置。
     * <p>
     * 白名单从 yaml 读，不硬编码：前端 vite.config.ts 里 host 是 127.0.0.1，
     * 而 localhost 和 127.0.0.1 在浏览器同源策略里是两个不同的源，写死哪个都会 403。
     * CorsFilter 在 DispatcherServlet 之前就返回，日志里查不到，所以放进配置方便改。
     * <p>
     * 配置那边写成逗号分隔的单个字符串，Spring 自动切分成 List。YAML 的 {@code - xxx}
     * 列表写法会变成 {@code cors.allowed-origins[0]} 这样的索引属性，
     * {@code ${cors.allowed-origins}} 取不到，启动直接报「无法解析占位符」。
     *
     * @param allowedOrigins 允许的前端来源，来自 {@code cors.allowed-origins}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        // 用 setAllowedOriginPatterns 而不是 setAllowedOrigins：前端没写 strictPort，
        // 5173 被占用时 Vite 会静默换到 5174，而 allowedOrigins 只认精确字符串，换端口就全 403。
        // 另外 allowCredentials(true) 时也不允许用 allowedOrigins 配通配符，Spring 会抛异常。
        // 生产环境要把 yaml 里的值换成精确域名，"*" 通配只适合本地开发。
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 带凭证的请求不允许通配源，必须列具体来源
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
