package com.asset.system.post.service.impl;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.post.dto.PostCreateDTO;
import com.asset.system.post.dto.PostQueryDTO;
import com.asset.system.post.entity.SysPost;
import com.asset.system.post.mapper.SysPostMapper;
import com.asset.system.post.service.SysPostService;
import com.asset.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 岗位管理 ServiceImpl */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost>
        implements SysPostService {

    private final SysUserMapper userMapper;

    @Override
    public IPage<SysPost> pageQuery(PostQueryDTO query) {
        return baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysPost>()
                        .like(StringUtils.hasText(query.getPostCode()), SysPost::getPostCode, query.getPostCode())
                        .like(StringUtils.hasText(query.getPostName()), SysPost::getPostName, query.getPostName())
                        .eq(query.getStatus() != null, SysPost::getStatus, query.getStatus())
                        .orderByAsc(SysPost::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(PostCreateDTO dto) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getPostCode, dto.getPostCode()));
        if (count > 0) throw new SysBizException(SysErrorCode.POST_CODE_EXISTS);
        SysPost post = new SysPost();
        post.setPostCode(dto.getPostCode());
        post.setPostName(dto.getPostName());
        post.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        post.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        post.setRemark(dto.getRemark());
        baseMapper.insert(post);
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostCreateDTO dto) {
        if (baseMapper.selectById(dto.getId()) == null) throw new SysBizException(SysErrorCode.POST_NOT_FOUND);
        update(new LambdaUpdateWrapper<SysPost>()
                .eq(SysPost::getId, dto.getId())
                .set(StringUtils.hasText(dto.getPostName()), SysPost::getPostName, dto.getPostName())
                .set(dto.getSortOrder() != null, SysPost::getSortOrder, dto.getSortOrder())
                .set(dto.getStatus() != null, SysPost::getStatus, dto.getStatus())
                .set(dto.getRemark() != null, SysPost::getRemark, dto.getRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        if (baseMapper.selectById(id) == null) throw new SysBizException(SysErrorCode.POST_NOT_FOUND);
        if (userMapper.countByPostId(id) > 0) throw new SysBizException(SysErrorCode.POST_HAS_USERS);
        removeById(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        update(new LambdaUpdateWrapper<SysPost>().eq(SysPost::getId, id).set(SysPost::getStatus, status));
    }
}
