package com.asset.common.mybatis.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 数据权限拦截器
 * 根据当前用户角色自动在SQL中追加数据过滤条件
 * 支持: 全部数据 / 本机构 / 本机构及下级 / 仅本人 / 自定义项目范围
 */
@Slf4j
public class DataPermissionInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms,
                            Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) {
        // TODO: 从SecurityContext获取当前用户的数据权限范围
        // 根据@DataScope注解判断是否需要拦截
        // 动态拼接 WHERE project_id IN (...) 或 org_id IN (...)
        // 多角色取并集
        log.trace("DataPermission interceptor - {}", ms.getId());
    }
}
