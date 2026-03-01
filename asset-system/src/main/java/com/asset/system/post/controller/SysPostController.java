package com.asset.system.post.controller;

import com.asset.common.model.R;
import com.asset.system.post.dto.PostCreateDTO;
import com.asset.system.post.dto.PostQueryDTO;
import com.asset.system.post.service.SysPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 岗位管理接口 */
@Tag(name = "03-岗位管理")
@RestController
@RequestMapping("/sys/posts")
@RequiredArgsConstructor
public class SysPostController {

    private final SysPostService postService;

    @Operation(summary = "岗位列表（分页）")
    @GetMapping
    public R<?> page(PostQueryDTO query) { return R.ok(postService.pageQuery(query)); }

    @Operation(summary = "所有正常状态岗位（下拉用）")
    @GetMapping("/list")
    public R<?> list() { return R.ok(postService.list()); }

    @Operation(summary = "新增岗位")
    @PostMapping
    public R<?> create(@Valid @RequestBody PostCreateDTO dto) { return R.ok(postService.createPost(dto)); }

    @Operation(summary = "更新岗位")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody PostCreateDTO dto) {
        dto.setId(id); postService.updatePost(dto); return R.ok();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) { postService.deletePost(id); return R.ok(); }

    @Operation(summary = "修改岗位状态")
    @PutMapping("/{id}/status")
    public R<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        postService.changeStatus(id, body.get("status")); return R.ok();
    }
}
