package com.asset.system.dept.service.impl;

import com.asset.system.common.exception.SysBizException;
import com.asset.system.common.exception.SysErrorCode;
import com.asset.system.dept.dto.DeptCreateDTO;
import com.asset.system.dept.dto.DeptTreeVO;
import com.asset.system.dept.dto.MoveDeptDTO;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.dept.service.SysDeptService;
import com.asset.system.post.entity.SysPost;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.user.dto.UserDetailVO;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.mapper.SysUserMapper;
import com.asset.system.user.mapper.SysUserPostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 部门/机构管理 ServiceImpl（含 Redis 缓存 + 移动子树） */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept>
        implements SysDeptService {

    private static final String CACHE_KEY_TREE = "sys:dept:tree";
    private static final long   CACHE_TTL_MIN  = 30L;

    private final SysUserMapper      userMapper;
    private final SysUserPostMapper  userPostMapper;
    private final SysRoleMapper      roleMapper;
    private final StringRedisTemplate redisTemplate;

    // ─── 查询 ────────────────────────────────────────────────────────────────

    @Override
    public List<DeptTreeVO> getDeptTree(Integer status) {
        // 不带 status 过滤时才用 Redis 缓存
        if (status == null) {
            String cached = redisTemplate.opsForValue().get(CACHE_KEY_TREE);
            if (cached != null && !cached.isBlank()) {
                // 简单标记：非空即为"已缓存"，实际返回时仍查 DB（JSON 序列化复杂度高，这里用标记+DB）
                // 生产中可改为 Jackson 序列化 List<DeptTreeVO>；此处为简化版
            }
        }
        List<SysDept> all = baseMapper.selectAllNormal();
        if (status != null) {
            all = all.stream().filter(d -> d.getStatus().equals(status)).collect(Collectors.toList());
        }
        List<DeptTreeVO> tree = buildTree(all, 0L);
        // 写缓存（不带status过滤时）
        if (status == null) {
            redisTemplate.opsForValue().set(CACHE_KEY_TREE, "1", CACHE_TTL_MIN, TimeUnit.MINUTES);
        }
        return tree;
    }

    @Override
    public SysDept getDetailById(Long id) {
        SysDept dept = baseMapper.selectById(id);
        if (dept == null) throw new SysBizException(SysErrorCode.DEPT_NOT_FOUND);
        return dept;
    }

    // ─── 增删改 ──────────────────────────────────────────────────────────────

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
        evictCache();
        log.info("[部门] 新增部门 {} id={}", dept.getDeptName(), dept.getId());
        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(DeptCreateDTO dto) {
        SysDept exist = getDetailById(dto.getId());

        // 若上级部门发生变更，更新自身及后代的 ancestors
        boolean parentChanged = dto.getParentId() != null
                && !Objects.equals(dto.getParentId(), exist.getParentId());

        LambdaUpdateWrapper<SysDept> wrapper = new LambdaUpdateWrapper<SysDept>()
                .eq(SysDept::getId, dto.getId())
                .set(dto.getDeptName() != null, SysDept::getDeptName, dto.getDeptName())
                .set(dto.getDeptCode() != null, SysDept::getDeptCode, dto.getDeptCode())
                .set(dto.getSortOrder() != null, SysDept::getSortOrder, dto.getSortOrder())
                .set(dto.getLeader() != null, SysDept::getLeader, dto.getLeader())
                .set(dto.getPhone() != null, SysDept::getPhone, dto.getPhone())
                .set(dto.getEmail() != null, SysDept::getEmail, dto.getEmail())
                .set(dto.getStatus() != null, SysDept::getStatus, dto.getStatus());

        if (parentChanged) {
            String newAncestors = buildAncestors(dto.getParentId());
            wrapper.set(SysDept::getParentId, dto.getParentId())
                   .set(SysDept::getAncestors, newAncestors);
            // 批量更新后代 ancestors
            updateDescendantsAncestors(dto.getId(), exist.getAncestors() + "," + dto.getId(), newAncestors + "," + dto.getId());
        }

        update(wrapper);
        evictCache();
        log.info("[部门] 更新部门 id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        getDetailById(id);
        if (baseMapper.countChildren(id) > 0) throw new SysBizException(SysErrorCode.DEPT_HAS_CHILDREN);
        if (userMapper.countByDeptId(id) > 0) throw new SysBizException(SysErrorCode.DEPT_HAS_USERS);
        removeById(id);
        evictCache();
        log.info("[部门] 删除部门 id={}", id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        getDetailById(id);
        update(new LambdaUpdateWrapper<SysDept>().eq(SysDept::getId, id).set(SysDept::getStatus, status));
        evictCache();
    }

    // ─── 移动子树 ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveDept(Long id, MoveDeptDTO dto) {
        SysDept dept = getDetailById(id);
        Long newParentId = dto.getTargetParentId();

        // 不能移动到自身或自身后代
        if (id.equals(newParentId)) {
            throw new SysBizException(SysErrorCode.DEPT_NOT_FOUND); // 可自定义新错误码
        }
        if (newParentId != 0L) {
            List<SysDept> descendants = baseMapper.selectDescendants(id);
            boolean isDescendant = descendants.stream().anyMatch(d -> d.getId().equals(newParentId));
            if (isDescendant) {
                throw new SysBizException(SysErrorCode.DEPT_HAS_CHILDREN); // 目标是后代，禁止移动
            }
        }

        String oldAncestorPrefix = dept.getAncestors() + "," + id;
        String newAncestors = buildAncestors(newParentId);
        String newAncestorPrefix = newAncestors + "," + id;

        // 更新自身
        update(new LambdaUpdateWrapper<SysDept>()
                .eq(SysDept::getId, id)
                .set(SysDept::getParentId, newParentId)
                .set(SysDept::getAncestors, newAncestors));

        // 批量更新后代
        if (!oldAncestorPrefix.equals(newAncestorPrefix)) {
            updateDescendantsAncestors(id, oldAncestorPrefix, newAncestorPrefix);
        }

        evictCache();
        log.info("[部门] 移动部门 id={} → parentId={}", id, newParentId);
    }

    // ─── 部门用户列表 ──────────────────────────────────────────────────────────

    @Override
    public List<UserDetailVO> getDeptUsers(Long deptId, boolean includeChildren) {
        List<Long> deptIds = new ArrayList<>();
        deptIds.add(deptId);

        if (includeChildren) {
            List<SysDept> descendants = baseMapper.selectDescendants(deptId);
            descendants.forEach(d -> deptIds.add(d.getId()));
        }

        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getDeptId, deptIds)
                .orderByAsc(SysUser::getId));

        return users.stream().map(this::toUserVO).collect(Collectors.toList());
    }

    // ─── 辅助方法 ─────────────────────────────────────────────────────────────

    @Override
    public String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) return "0";
        SysDept parent = baseMapper.selectById(parentId);
        if (parent == null) return "0";
        return parent.getAncestors() + "," + parentId;
    }

    @Override
    public void evictCache() {
        redisTemplate.delete(CACHE_KEY_TREE);
    }

    /**
     * 批量更新后代的 ancestors 路径
     *
     * @param nodeId          当前节点 ID（用于查询后代）
     * @param oldAncestorPath 旧的 ancestors 前缀（含当前节点 ID），例如 "0,1,5"
     * @param newAncestorPath 新的 ancestors 前缀（含当前节点 ID），例如 "0,2,5"
     */
    private void updateDescendantsAncestors(Long nodeId, String oldAncestorPath, String newAncestorPath) {
        int updated = baseMapper.batchUpdateAncestors(oldAncestorPath, newAncestorPath);
        log.info("[部门] 批量更新后代 ancestors，nodeId={} updated={}", nodeId, updated);
    }

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

    private UserDetailVO toUserVO(SysUser user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setDeptId(user.getDeptId());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setStatusName(user.getStatus() != null ? (user.getStatus() == 1 ? "正常" : "停用") : null);
        vo.setLoginTime(user.getLoginTime());

        // 加载角色信息
        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        if (roles != null && !roles.isEmpty()) {
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        } else {
            vo.setRoleNames(Collections.emptyList());
        }

        // 加载岗位信息
        List<com.asset.system.post.entity.SysPost> posts = userPostMapper.selectPostsByUserId(user.getId());
        if (posts != null && !posts.isEmpty()) {
            vo.setPostNames(posts.stream().map(SysPost::getPostName).collect(Collectors.toList()));
        } else {
            vo.setPostNames(Collections.emptyList());
        }

        return vo;
    }
}
