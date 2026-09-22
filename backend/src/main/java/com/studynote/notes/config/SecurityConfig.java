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

/**
 * @ClassName Security配置类
 * @Description ToDo
 * @LastChangeDate 2024-12-17 15:40
 * @Version v1.0
 */
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
     * 【为什么白名单从配置读，而不是写死在代码里】
     * 原来这里硬编码了 {@code "http://localhost:5173"}，结果前端起不来 ——
     * 因为 vite.config.ts 里 {@code host: '127.0.0.1'}，浏览器地址栏是
     * {@code http://127.0.0.1:5173}，而 <b>localhost 和 127.0.0.1 在浏览器的
     * 同源策略里是两个不同的源</b>。于是每个请求都被本过滤器拒成 403。
     * <p>
     * 这个 bug 特别难查，因为：
     * <ol>
     *   <li>CorsFilter 在 DispatcherServlet <b>之前</b>就返回了，
     *       请求根本没进 Spring MVC —— 应用日志里一条记录都没有，
     *       你在日志里怎么找都找不到，会误以为是"请求没发出去"；</li>
     *   <li>返回的是空的 403，不是项目统一的 {@code {"code":..,"message":..}} JSON，
     *       所以前端也拿不到任何有用信息。</li>
     * </ol>
     * 放进配置后，换端口 / 换域名 / 上生产只需改 yaml。
     * <p>
     * 【为什么用 @Value 注入 List，而不是去读 YAML 列表】
     * YAML 的 {@code - xxx} 写法在 Spring 里会变成 {@code cors.allowed-origins[0]}、
     * {@code [1]} 这样的<b>索引属性</b>，而 {@code ${cors.allowed-origins}} 取的是
     * 不带下标的键，取不到 → 报「无法解析占位符」直接启动失败。
     * 所以配置那边写成逗号分隔的单个字符串，Spring 会自动切分成 List。
     *
     * @param allowedOrigins 允许的前端来源，来自 {@code cors.allowed-origins}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        // 【为什么是 setAllowedOriginPatterns 而不是 setAllowedOrigins】
        // 前端 vite.config.ts 里既没写 port 也没写 strictPort，所以 5173 一旦被占用，
        // Vite 会【静默换成 5174、5175…】。而 allowedOrigins 只认精确字符串，
        // 换成 5174 后每个请求都会 403 —— 而且和上次那个 localhost/127.0.0.1 的坑一样，
        // CorsFilter 在 DispatcherServlet 之前就返回了，应用日志里一条记录都没有。
        // allowedOriginPatterns 支持 "http://localhost:*" 这种按端口通配，
        // 换端口不用再改配置、不用重启。
        //
        // 注意：allowCredentials(true) 时【不能】用 allowedOrigins 配通配符，
        // Spring 会直接抛异常；通配必须走 allowedOriginPatterns —— 这也是这里换方法的原因。
        // 生产环境请把 yaml 里的值换回精确域名，"*" 通配只适合本地开发。
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // allowCredentials(true) 时【不能】把 allowedOrigins 设成 "*"：
        // 带凭证的请求不允许通配源，Spring 会直接抛异常。
        // 这也是这里必须列具体来源、不能图省事写 "*" 的原因。
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
