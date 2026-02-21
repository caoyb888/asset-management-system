package com.asset.common.security.config;

import com.asset.common.security.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Security 公共自动配置
 * <p>
 * 各微服务引入 asset-common-security 后自动生效：<br>
 * - 无状态 JWT 鉴权<br>
 * - 公开接口白名单（登录、Swagger、Actuator）<br>
 * - 401/403 JSON 响应
 * </p>
 */
@AutoConfiguration
@EnableMethodSecurity
public class SecurityAutoConfiguration {

    /** 公开路径白名单 */
    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/publicKey",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .anyRequest().authenticated())
            // JWT 过滤器插入在 UsernamePasswordAuthenticationFilter 之前
            .addFilterBefore(new JwtAuthenticationFilter(),
                    UsernamePasswordAuthenticationFilter.class)
            // 401 未认证：返回 JSON
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) -> {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                        Map<String, Object> body = new HashMap<>();
                        body.put("code", 401);
                        body.put("msg", "未登录或登录已过期");
                        body.put("data", null);
                        res.getWriter().write(objectMapper.writeValueAsString(body));
                    })
                    .accessDeniedHandler((req, res, e) -> {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                        Map<String, Object> body = new HashMap<>();
                        body.put("code", 403);
                        body.put("msg", "无访问权限");
                        body.put("data", null);
                        res.getWriter().write(objectMapper.writeValueAsString(body));
                    }));

        return http.build();
    }
}
