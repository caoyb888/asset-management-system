package com.asset.base.controller;

import com.asset.base.entity.SysCompany;
import com.asset.base.mapper.SysCompanyMapper;
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
 * 公司接口（下拉选项等基础查询）
 */
@Tag(name = "公司管理")
@RestController
@RequestMapping("/base/companies")
@RequiredArgsConstructor
public class SysCompanyController {

    private final SysCompanyMapper sysCompanyMapper;

    /** 获取启用状态的公司列表（用于下拉选择） */
    @Operation(summary = "公司列表")
    @GetMapping("/list")
    public R<List<SysCompany>> list() {
        List<SysCompany> list = sysCompanyMapper.selectList(
                new LambdaQueryWrapper<SysCompany>()
                        .eq(SysCompany::getStatus, 1)
                        .orderByAsc(SysCompany::getId)
        );
        return R.ok(list);
    }
}
