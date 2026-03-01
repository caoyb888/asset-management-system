package com.asset.system.common.datascope;

import com.asset.common.model.R;
import com.asset.common.security.util.SecurityUtil;
import com.asset.system.common.enums.DataScope;
import com.asset.system.dept.entity.SysDept;
import com.asset.system.dept.mapper.SysDeptMapper;
import com.asset.system.role.entity.SysRole;
import com.asset.system.role.mapper.SysRoleDataMapper;
import com.asset.system.role.mapper.SysRoleMapper;
import com.asset.system.user.entity.SysUser;
import com.asset.system.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 数据权限调试接口 */
@Tag(name = "07-数据权限")
@RestController
@RequestMapping("/sys/data-scope")
@RequiredArgsConstructor
public class DataScopeController {

    private final SysUserMapper     userMapper;
    private final SysRoleMapper     roleMapper;
    private final SysRoleDataMapper roleDataMapper;
    private final SysDeptMapper     deptMapper;

    @Operation(summary = "查询当前用户的数据权限信息")
    @GetMapping("/current")
    public R<DataScopeVO> currentScope() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null || userId == 0L) return R.ok(DataScopeVO.ofAdmin(0L, "未登录"));

        SysUser user = userMapper.selectById(userId);
        if (user == null) return R.ok(DataScopeVO.ofAdmin(userId, "用户不存在"));

        List<SysRole> roles = roleMapper.selectByUserId(userId);

        DataScopeVO vo = new DataScopeVO();
        vo.setUserId(userId);
        vo.setDeptId(user.getDeptId());
        vo.setRoles(roles.stream().map(r -> {
            RoleScopeVO rs = new RoleScopeVO();
            rs.setRoleId(r.getId());
            rs.setRoleCode(r.getRoleCode());
            rs.setRoleName(r.getRoleName());
            rs.setDataScope(r.getDataScope());
            rs.setDataScopeName(DataScope.of(r.getDataScope() != null ? r.getDataScope() : 1).getDesc());
            if (Integer.valueOf(2).equals(r.getDataScope())) {
                rs.setCustomDeptIds(roleDataMapper.selectDeptIdsByRoleId(r.getId()));
            }
            return rs;
        }).collect(Collectors.toList()));

        // 解析最终有效范围
        boolean isAdmin = roles.stream().anyMatch(r ->
                "SUPER_ADMIN".equals(r.getRoleCode()) || Integer.valueOf(1).equals(r.getDataScope()));
        vo.setAdmin(isAdmin);

        if (!isAdmin) {
            Set<Long> deptIdSet = new LinkedHashSet<>();
            boolean selfOnly = false;
            for (SysRole role : roles) {
                int ds = role.getDataScope() != null ? role.getDataScope() : 1;
                switch (DataScope.of(ds)) {
                    case ALL: isAdmin = true; break;
                    case DEPT_AND_CHILD:
                        if (user.getDeptId() != null) {
                            deptIdSet.add(user.getDeptId());
                            deptMapper.selectDescendants(user.getDeptId()).forEach(d -> deptIdSet.add(d.getId()));
                        }
                        break;
                    case DEPT:
                        if (user.getDeptId() != null) deptIdSet.add(user.getDeptId());
                        break;
                    case CUSTOM:
                        List<Long> cids = roleDataMapper.selectDeptIdsByRoleId(role.getId());
                        if (cids != null) deptIdSet.addAll(cids);
                        break;
                    case SELF:
                        selfOnly = true; break;
                }
            }
            vo.setAdmin(isAdmin);
            if (!isAdmin) {
                vo.setSelfOnly(!isAdmin && deptIdSet.isEmpty() && selfOnly);
                vo.setEffectiveDeptIds(deptIdSet.isEmpty() ? null : new ArrayList<>(deptIdSet));

                // 加载部门名称
                if (vo.getEffectiveDeptIds() != null) {
                    vo.setEffectiveDeptNames(vo.getEffectiveDeptIds().stream()
                            .map(id -> {
                                SysDept dept = deptMapper.selectById(id);
                                return dept != null ? dept.getDeptName() : "ID:" + id;
                            }).collect(Collectors.toList()));
                }
            }
        }

        return R.ok(vo);
    }

    @Data
    public static class DataScopeVO {
        private Long userId;
        private Long deptId;
        private boolean admin;
        private boolean selfOnly;
        private List<Long> effectiveDeptIds;
        private List<String> effectiveDeptNames;
        private List<RoleScopeVO> roles;

        static DataScopeVO ofAdmin(Long userId, String note) {
            DataScopeVO vo = new DataScopeVO();
            vo.userId = userId;
            vo.admin = true;
            return vo;
        }
    }

    @Data
    public static class RoleScopeVO {
        private Long roleId;
        private String roleCode;
        private String roleName;
        private Integer dataScope;
        private String dataScopeName;
        private List<Long> customDeptIds;
    }
}
