package com.asset.system.post.service;

import com.asset.system.post.dto.PostCreateDTO;
import com.asset.system.post.dto.PostQueryDTO;
import com.asset.system.post.entity.SysPost;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** 岗位管理 Service */
public interface SysPostService extends IService<SysPost> {
    IPage<SysPost> pageQuery(PostQueryDTO query);
    /** 查询所有启用状态的岗位（下拉/选择器使用） */
    java.util.List<SysPost> listEnabled();
    Long createPost(PostCreateDTO dto);
    void updatePost(PostCreateDTO dto);
    void deletePost(Long id);
    void changeStatus(Long id, Integer status);
}
