package com.asset.system.code.controller;

import com.asset.common.model.R;
import com.asset.system.code.dto.CodeRuleCreateDTO;
import com.asset.system.code.dto.CodeRuleQueryDTO;
import com.asset.system.code.service.SysCodeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 业务编码规则接口 */
@Tag(name = "08-编码规则管理")
@RestController
@RequestMapping("/sys/code-rules")
@RequiredArgsConstructor
public class SysCodeRuleController {

    private final SysCodeRuleService codeRuleService;

    @Operation(summary = "分页查询编码规则")
    @GetMapping
    public R<?> page(CodeRuleQueryDTO query) {
        return R.ok(codeRuleService.pageQuery(query));
    }

    @Operation(summary = "新增编码规则")
    @PostMapping
    public R<?> create(@Valid @RequestBody CodeRuleCreateDTO dto) {
        return R.ok(codeRuleService.createRule(dto));
    }

    @Operation(summary = "更新编码规则")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody CodeRuleCreateDTO dto) {
        dto.setId(id);
        codeRuleService.updateRule(dto);
        return R.ok();
    }

    @Operation(summary = "删除编码规则")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        codeRuleService.deleteRule(id);
        return R.ok();
    }

    @Operation(summary = "修改编码规则状态")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        codeRuleService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "重置序列号（归零）")
    @PutMapping("/{id}/reset-seq")
    public R<?> resetSeq(@PathVariable Long id) {
        codeRuleService.resetSeq(id);
        return R.ok();
    }

    @Operation(summary = "生成下一个业务编码（测试/预览用）")
    @GetMapping("/generate/{ruleKey}")
    public R<?> generate(@PathVariable String ruleKey) {
        return R.ok(codeRuleService.generateCode(ruleKey));
    }
}
