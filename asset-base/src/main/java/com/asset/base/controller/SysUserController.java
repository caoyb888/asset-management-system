package com.asset.base.controller;

import com.asset.base.entity.SysUser;
import com.asset.base.mapper.SysUserMapper;
import com.asset.common.model.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口（下拉选项等基础查询）
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/base/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserMapper sysUserMapper;

    /** 获取启用状态的用户列表（用于下拉选择） */
    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public R<List<SysUser>> list() {
        List<SysUser> list = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1)
                        .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName)
                        .orderByAsc(SysUser::getId)
        );
        return R.ok(list);
    }
}
