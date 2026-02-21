package com.asset.common.security.filter;

import com.asset.common.security.jwt.JwtUtil;
import com.asset.common.security.util.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * <p>从 Authorization Header 解析 JWT，验证后设置 Spring Security 上下文</p>
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(JwtUtil.HEADER_NAME);
        String token = JwtUtil.extractToken(header);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = JwtUtil.parseToken(token);
            if (claims != null) {
                Long userId = claims.get("userId", Long.class);
                String username = claims.getSubject();
                // 简化版：角色暂时固定为 ROLE_USER，后续可扩展为从 Redis 读取完整权限
                LoginUser loginUser = new LoginUser(userId, username, "",
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
