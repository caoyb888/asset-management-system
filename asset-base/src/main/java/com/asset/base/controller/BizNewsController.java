package com.asset.base.controller;

import com.asset.base.model.dto.NewsQuery;
import com.asset.base.model.dto.NewsSaveDTO;
import com.asset.base.model.vo.NewsVO;
import com.asset.base.service.BizNewsService;
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
 * 新闻资讯管理 Controller
 *
 * <pre>
 * GET    /base/news              分页查询
 * GET    /base/news/{id}         资讯详情
 * POST   /base/news              新增资讯
 * PUT    /base/news/{id}         编辑资讯
 * DELETE /base/news/{id}         逻辑删除
 * PUT    /base/news/{id}/publish   上架资讯
 * PUT    /base/news/{id}/unpublish 下架资讯
 * </pre>
 */
@Tag(name = "新闻资讯管理", description = "基础数据-新闻资讯增删改查及上下架管理")
@RestController
@RequestMapping("/base/news")
@RequiredArgsConstructor
public class BizNewsController {

    private final BizNewsService newsService;

    @Operation(summary = "分页查询新闻资讯列表")
    @GetMapping
    @OperLog(module = "新闻资讯管理", action = "分页查询", type = OperLog.OperType.QUERY)
    public R<IPage<NewsVO>> page(NewsQuery query) {
        return R.ok(newsService.pageNews(query));
    }

    @Operation(summary = "查询新闻资讯详情")
    @GetMapping("/{id}")
    @OperLog(module = "新闻资讯管理", action = "查询详情", type = OperLog.OperType.QUERY)
    public R<NewsVO> detail(
            @Parameter(description = "资讯ID") @PathVariable Long id) {
        return R.ok(newsService.getNewsById(id));
    }

    @Operation(summary = "新增新闻资讯")
    @PostMapping
    @OperLog(module = "新闻资讯管理", action = "新增", type = OperLog.OperType.CREATE)
    public R<Long> create(@Valid @RequestBody NewsSaveDTO dto) {
        return R.ok(newsService.createNews(dto));
    }

    @Operation(summary = "编辑新闻资讯")
    @PutMapping("/{id}")
    @OperLog(module = "新闻资讯管理", action = "编辑", type = OperLog.OperType.UPDATE)
    public R<Void> update(
            @Parameter(description = "资讯ID") @PathVariable Long id,
            @Valid @RequestBody NewsSaveDTO dto) {
        newsService.updateNews(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "删除新闻资讯（逻辑删除）")
    @DeleteMapping("/{id}")
    @OperLog(module = "新闻资讯管理", action = "删除", type = OperLog.OperType.DELETE)
    public R<Void> delete(
            @Parameter(description = "资讯ID") @PathVariable Long id) {
        newsService.deleteNews(id);
        return R.ok(null);
    }

    @Operation(summary = "上架新闻资讯")
    @PutMapping("/{id}/publish")
    @OperLog(module = "新闻资讯管理", action = "上架", type = OperLog.OperType.UPDATE)
    public R<Void> publish(
            @Parameter(description = "资讯ID") @PathVariable Long id) {
        newsService.publishNews(id);
        return R.ok(null);
    }

    @Operation(summary = "下架新闻资讯")
    @PutMapping("/{id}/unpublish")
    @OperLog(module = "新闻资讯管理", action = "下架", type = OperLog.OperType.UPDATE)
    public R<Void> unpublish(
            @Parameter(description = "资讯ID") @PathVariable Long id) {
        newsService.unpublishNews(id);
        return R.ok(null);
    }
}
