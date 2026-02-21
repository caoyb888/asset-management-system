package com.asset.common.mybatis.handler;

import com.asset.common.security.util.SecurityUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 审计字段自动填充
 * <p>
 * INSERT 时填充：created_by、created_at、updated_by、updated_at
 * UPDATE 时填充：updated_by、updated_at
 * </p>
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        this.strictInsertFill(metaObject, "createdBy",  Long.class,          userId);
        this.strictInsertFill(metaObject, "createdAt",  LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedBy",  Long.class,          userId);
        this.strictInsertFill(metaObject, "updatedAt",  LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        this.strictUpdateFill(metaObject, "updatedBy", Long.class,          userId);
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }
}
