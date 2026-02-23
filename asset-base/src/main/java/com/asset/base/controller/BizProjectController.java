package com.asset.base.controller;

import com.asset.base.entity.BizProject;
import com.asset.base.model.dto.ProjectBankDTO;
import com.asset.base.model.dto.ProjectContractDTO;
import com.asset.base.model.dto.ProjectFinanceContactDTO;
import com.asset.base.model.dto.ProjectQuery;
import com.asset.base.model.dto.ProjectSaveDTO;
import com.asset.base.model.vo.ProjectBankVO;
import com.asset.base.model.vo.ProjectContractVO;
import com.asset.base.model.vo.ProjectFinanceContactVO;
import com.asset.base.model.vo.ProjectVO;
import com.asset.base.service.BizProjectService;
import com.asset.common.log.annotation.OperLog;
import com.asset.common.model.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "获取项目下拉列表")
    @GetMapping("/list")
    public R<List<BizProject>> list() {
        List<BizProject> list = projectService.list(
                new LambdaQueryWrapper<BizProject>()
                        .eq(BizProject::getIsDeleted, 0)
                        .select(BizProject::getId, BizProject::getProjectName, BizProject::getProjectCode)
                        .orderByAsc(BizProject::getId)
        );
        return R.ok(list);
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

    /* ------------------------------------------------------------------ */
    /* 合同甲方信息                                                          */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "获取项目合同甲方信息")
    @GetMapping("/{id}/contracts")
    @OperLog(module = "项目管理", action = "查询合同甲方", type = OperLog.OperType.QUERY)
    public R<ProjectContractVO> getContract(
            @Parameter(description = "项目ID") @PathVariable Long id) {
        return R.ok(projectService.getContract(id));
    }

    @Operation(summary = "保存项目合同甲方信息（存在则更新，否则新增）")
    @PutMapping("/{id}/contracts")
    @OperLog(module = "项目管理", action = "保存合同甲方", type = OperLog.OperType.UPDATE)
    public R<Void> saveContract(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @RequestBody ProjectContractDTO dto) {
        projectService.saveContract(id, dto);
        return R.ok(null);
    }

    /* ------------------------------------------------------------------ */
    /* 财务联系人                                                            */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "查询项目财务联系人列表")
    @GetMapping("/{id}/finance-contacts")
    @OperLog(module = "项目管理", action = "查询财务联系人", type = OperLog.OperType.QUERY)
    public R<List<ProjectFinanceContactVO>> listFinanceContacts(
            @Parameter(description = "项目ID") @PathVariable Long id) {
        return R.ok(projectService.listFinanceContacts(id));
    }

    @Operation(summary = "新增财务联系人")
    @PostMapping("/{id}/finance-contacts")
    @OperLog(module = "项目管理", action = "新增财务联系人", type = OperLog.OperType.CREATE)
    public R<Long> addFinanceContact(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Valid @RequestBody ProjectFinanceContactDTO dto) {
        return R.ok(projectService.addFinanceContact(id, dto));
    }

    @Operation(summary = "更新财务联系人")
    @PutMapping("/{id}/finance-contacts/{cid}")
    @OperLog(module = "项目管理", action = "更新财务联系人", type = OperLog.OperType.UPDATE)
    public R<Void> updateFinanceContact(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Parameter(description = "联系人ID") @PathVariable Long cid,
            @Valid @RequestBody ProjectFinanceContactDTO dto) {
        projectService.updateFinanceContact(cid, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除财务联系人")
    @DeleteMapping("/{id}/finance-contacts/{cid}")
    @OperLog(module = "项目管理", action = "删除财务联系人", type = OperLog.OperType.DELETE)
    public R<Void> deleteFinanceContact(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Parameter(description = "联系人ID") @PathVariable Long cid) {
        projectService.deleteFinanceContact(cid);
        return R.ok(null);
    }

    /* ------------------------------------------------------------------ */
    /* 银行账号                                                              */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "查询项目银行账号列表")
    @GetMapping("/{id}/banks")
    @OperLog(module = "项目管理", action = "查询银行账号", type = OperLog.OperType.QUERY)
    public R<List<ProjectBankVO>> listBanks(
            @Parameter(description = "项目ID") @PathVariable Long id) {
        return R.ok(projectService.listBanks(id));
    }

    @Operation(summary = "新增银行账号")
    @PostMapping("/{id}/banks")
    @OperLog(module = "项目管理", action = "新增银行账号", type = OperLog.OperType.CREATE)
    public R<Long> addBank(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Valid @RequestBody ProjectBankDTO dto) {
        return R.ok(projectService.addBank(id, dto));
    }

    @Operation(summary = "更新银行账号")
    @PutMapping("/{id}/banks/{bid}")
    @OperLog(module = "项目管理", action = "更新银行账号", type = OperLog.OperType.UPDATE)
    public R<Void> updateBank(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Parameter(description = "银行账号ID") @PathVariable Long bid,
            @Valid @RequestBody ProjectBankDTO dto) {
        projectService.updateBank(bid, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除银行账号")
    @DeleteMapping("/{id}/banks/{bid}")
    @OperLog(module = "项目管理", action = "删除银行账号", type = OperLog.OperType.DELETE)
    public R<Void> deleteBank(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Parameter(description = "银行账号ID") @PathVariable Long bid) {
        projectService.deleteBank(bid);
        return R.ok(null);
    }
}
