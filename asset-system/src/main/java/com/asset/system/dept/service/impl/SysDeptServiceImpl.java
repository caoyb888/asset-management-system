package com.asset.system.dept.service.impl;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.dept.dto.DeptCreateDTO;
import com.asset.system.dept.dto.DeptTreeVO;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.dept.service.SysDeptService;
import com.asset.system.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** 部门/机构管理 ServiceImpl */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept>
        implements SysDeptService {

    private final SysUserMapper userMapper;

    @Override
    public List<DeptTreeVO> getDeptTree(Integer status) {
        List<SysDept> all = baseMapper.selectAllNormal();
        if (status != null) {
            all = all.stream().filter(d -> d.getStatus().equals(status)).collect(Collectors.toList());
        }
        return buildTree(all, 0L);
    }

    @Override
    public SysDept getDetailById(Long id) {
        SysDept dept = baseMapper.selectById(id);
        if (dept == null) throw new SysBizException(SysErrorCode.DEPT_NOT_FOUND);
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDept(DeptCreateDTO dto) {
        SysDept dept = new SysDept();
        dept.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        dept.setAncestors(buildAncestors(dept.getParentId()));
        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        baseMapper.insert(dept);
        log.info("[部门] 新增部门 {} id={}", dept.getDeptName(), dept.getId());
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(DeptCreateDTO dto) {
        getDetailById(dto.getId());
        LambdaUpdateWrapper<SysDept> wrapper = new LambdaUpdateWrapper<SysDept>()
                .eq(SysDept::getId, dto.getId())
                .set(dto.getDeptName() != null, SysDept::getDeptName, dto.getDeptName())
                .set(dto.getDeptCode() != null, SysDept::getDeptCode, dto.getDeptCode())
                .set(dto.getSortOrder() != null, SysDept::getSortOrder, dto.getSortOrder())
                .set(dto.getLeader() != null, SysDept::getLeader, dto.getLeader())
                .set(dto.getPhone() != null, SysDept::getPhone, dto.getPhone())
                .set(dto.getEmail() != null, SysDept::getEmail, dto.getEmail())
                .set(dto.getStatus() != null, SysDept::getStatus, dto.getStatus());
        update(wrapper);
        log.info("[部门] 更新部门 id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        getDetailById(id);
        if (baseMapper.countChildren(id) > 0) throw new SysBizException(SysErrorCode.DEPT_HAS_CHILDREN);
        if (userMapper.countByDeptId(id) > 0) throw new SysBizException(SysErrorCode.DEPT_HAS_USERS);
        removeById(id);
        log.info("[部门] 删除部门 id={}", id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        getDetailById(id);
        update(new LambdaUpdateWrapper<SysDept>().eq(SysDept::getId, id).set(SysDept::getStatus, status));
    }

    @Override
    public String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) return "0";
        SysDept parent = baseMapper.selectById(parentId);
        if (parent == null) return "0";
        return parent.getAncestors() + "," + parentId;
    }

    // ─── 私有辅助 ────────────────────────────────────────────────────────────

    private List<DeptTreeVO> buildTree(List<SysDept> all, Long parentId) {
        List<DeptTreeVO> result = new ArrayList<>();
        for (SysDept dept : all) {
            if (dept.getParentId().equals(parentId)) {
                DeptTreeVO vo = toTreeVO(dept);
                List<DeptTreeVO> children = buildTree(all, dept.getId());
                vo.setChildren(children.isEmpty() ? null : children);
                result.add(vo);
            }
        }
        return result;
    }

    private DeptTreeVO toTreeVO(SysDept dept) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setDeptName(dept.getDeptName());
        vo.setDeptCode(dept.getDeptCode());
        vo.setSortOrder(dept.getSortOrder());
        vo.setLeader(dept.getLeader());
        vo.setStatus(dept.getStatus());
        return vo;
    }
}
