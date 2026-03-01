package com.asset.system.dict.controller;

import com.asset.common.model.R;
import com.asset.system.dict.dto.DictDataCreateDTO;
import com.asset.system.dict.dto.DictQueryDTO;
import com.asset.system.dict.dto.DictTypeCreateDTO;
import com.asset.system.dict.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 业务字典接口 */
@Tag(name = "06-业务字典")
@RestController
@RequestMapping("/sys/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    // ─── 字典类型 ─────────────────────────────────────────────────────────────

    @Operation(summary = "字典类型列表（分页）")
    @GetMapping("/types")
    public R<?> pageType(DictQueryDTO query) { return R.ok(dictService.pageQueryType(query)); }

    @Operation(summary = "新增字典类型")
    @PostMapping("/types")
    public R<?> createType(@Valid @RequestBody DictTypeCreateDTO dto) { return R.ok(dictService.createType(dto)); }

    @Operation(summary = "更新字典类型")
    @PutMapping("/types/{id}")
    public R<?> updateType(@PathVariable Long id, @Valid @RequestBody DictTypeCreateDTO dto) {
        dto.setId(id); dictService.updateType(dto); return R.ok();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/types/{id}")
    public R<?> deleteType(@PathVariable Long id) { dictService.deleteType(id); return R.ok(); }

    // ─── 字典数据 ─────────────────────────────────────────────────────────────

    @Operation(summary = "获取字典数据（按类型）")
    @GetMapping("/data/{dictType}")
    public R<?> listData(@PathVariable String dictType) { return R.ok(dictService.listData(dictType)); }

    @Operation(summary = "新增字典数据")
    @PostMapping("/data")
    public R<?> createData(@Valid @RequestBody DictDataCreateDTO dto) { return R.ok(dictService.createData(dto)); }

    @Operation(summary = "更新字典数据")
    @PutMapping("/data/{id}")
    public R<?> updateData(@PathVariable Long id, @Valid @RequestBody DictDataCreateDTO dto) {
        dto.setId(id); dictService.updateData(dto); return R.ok();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/data/{id}")
    public R<?> deleteData(@PathVariable Long id) { dictService.deleteData(id); return R.ok(); }
}
