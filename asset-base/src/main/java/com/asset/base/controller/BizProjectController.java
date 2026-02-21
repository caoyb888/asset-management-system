package com.asset.base.controller;

import com.asset.base.model.dto.ProjectQuery;
import com.asset.base.model.dto.ProjectSaveDTO;
import com.asset.base.model.vo.ProjectVO;
import com.asset.base.service.BizProjectService;
import com.asset.common.log.annotation.OperLog;
import com.asset.common.model.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 项目管理 Controller
 *
 * <pre>
 * GET    /api/base/projects         分页查询
 * POST   /api/base/projects         新增项目
 * GET    /api/base/projects/{id}    项目详情
 * PUT    /api/base/projects/{id}    编辑项目
 * DELETE /api/base/projects/{id}    逻辑删除
 * </pre>
 */
@Tag(name = "项目管理", description = "基础数据-项目增删改查")
@RestController
@RequestMapping("/base/projects")
@RequiredArgsConstructor
public class BizProjectController {

    private final BizProjectService projectService;

    /* ------------------------------------------------------------------ */
    /* 查询                                                                  */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "分页查询项目列表")
    @GetMapping
    @OperLog(module = "项目管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<ProjectVO>> page(ProjectQuery query) {
        return R.ok(projectService.pageProject(query));
    }

    @Operation(summary = "查询项目详情")
    @GetMapping("/{id}")
    @OperLog(module = "项目管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<ProjectVO> detail(
            @Parameter(description = "项目ID") @PathVariable Long id) {
        return R.ok(projectService.getProjectById(id));
    }

    /* ------------------------------------------------------------------ */
    /* 新增                                                                  */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "新增项目")
    @PostMapping
    @OperLog(module = "项目管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody ProjectSaveDTO dto) {
        return R.ok(projectService.createProject(dto));
    }

    /* ------------------------------------------------------------------ */
    /* 编辑                                                                  */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "编辑项目")
    @PutMapping("/{id}")
    @OperLog(module = "项目管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Valid @RequestBody ProjectSaveDTO dto) {
        projectService.updateProject(id, dto);
        return R.ok(null);
    }

    /* ------------------------------------------------------------------ */
    /* 删除                                                                  */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "删除项目（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "项目管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "项目ID") @PathVariable Long id) {
        projectService.deleteProject(id);
        return R.ok(null);
    }
}
