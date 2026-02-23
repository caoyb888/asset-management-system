package com.asset.base.controller;

import com.asset.base.model.dto.NoticeQuery;
import com.asset.base.model.dto.NoticeSaveDTO;
import com.asset.base.model.vo.NoticeVO;
import com.asset.base.service.BizNoticeService;
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
 * 通知公告管理 Controller
 *
 * <pre>
 * GET    /base/notices              分页查询
 * GET    /base/notices/{id}         公告详情
 * POST   /base/notices              新增公告
 * PUT    /base/notices/{id}         编辑公告
 * DELETE /base/notices/{id}         逻辑删除
 * PUT    /base/notices/{id}/publish   发布公告
 * PUT    /base/notices/{id}/unpublish 下架公告
 * </pre>
 */
@Tag(name = "通知公告管理", description = "基础数据-通知公告增删改查及发布管理")
@RestController
@RequestMapping("/base/notices")
@RequiredArgsConstructor
public class BizNoticeController {

    private final BizNoticeService noticeService;

    @Operation(summary = "分页查询通知公告列表")
    @GetMapping
    @OperLog(module = "通知公告管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<NoticeVO>> page(NoticeQuery query) {
        return R.ok(noticeService.pageNotice(query));
    }

    @Operation(summary = "查询通知公告详情")
    @GetMapping("/{id}")
    @OperLog(module = "通知公告管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<NoticeVO> detail(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        return R.ok(noticeService.getNoticeById(id));
    }

    @Operation(summary = "新增通知公告")
    @PostMapping
    @OperLog(module = "通知公告管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody NoticeSaveDTO dto) {
        return R.ok(noticeService.createNotice(dto));
    }

    @Operation(summary = "编辑通知公告")
    @PutMapping("/{id}")
    @OperLog(module = "通知公告管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "公告ID") @PathVariable Long id,
            @Valid @RequestBody NoticeSaveDTO dto) {
        noticeService.updateNotice(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除通知公告（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "通知公告管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        noticeService.deleteNotice(id);
        return R.ok(null);
    }

    @Operation(summary = "发布通知公告")
    @PutMapping("/{id}/publish")
    @OperLog(module = "通知公告管理", action = "发布", type = OperLog.OperType.UPDATE)
    public R<Void> publish(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        noticeService.publishNotice(id);
        return R.ok(null);
    }

    @Operation(summary = "下架通知公告")
    @PutMapping("/{id}/unpublish")
    @OperLog(module = "通知公告管理", action = "下架", type = OperLog.OperType.UPDATE)
    public R<Void> unpublish(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        noticeService.unpublishNotice(id);
        return R.ok(null);
    }
}
