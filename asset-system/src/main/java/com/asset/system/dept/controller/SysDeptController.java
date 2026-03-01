package com.asset.system.dept.controller;

import com.asset.common.model.R;
import com.asset.system.dept.dto.DeptCreateDTO;
import com.asset.system.dept.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 机构管理接口 */
@Tag(name = "02-机构管理")
@RestController
@RequestMapping("/sys/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @Operation(summary = "部门树列表")
    @GetMapping
    public R<?> tree(@RequestParam(required = false) Integer status) { return R.ok(deptService.getDeptTree(status)); }

    @Operation(summary = "部门详情")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) { return R.ok(deptService.getDetailById(id)); }

    @Operation(summary = "新增部门")
    @PostMapping
    public R<?> create(@Valid @RequestBody DeptCreateDTO dto) { return R.ok(deptService.createDept(dto)); }

    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody DeptCreateDTO dto) {
        dto.setId(id); deptService.updateDept(dto); return R.ok();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) { deptService.deleteDept(id); return R.ok(); }

    @Operation(summary = "修改部门状态")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        deptService.changeStatus(id, body.get("status")); return R.ok();
    }
}
