package com.asset.report.common.permission;

import com.asset.common.model.R;
import com.asset.common.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 报表权限查询接口
 * <p>
 * 供前端在初始化时查询当前用户的报表权限信息，
 * 用于控制菜单可见性和数据展示方式（脱敏/原值）。
 * </p>
 */
@Tag(name = "报表通用-权限", description = "查询当前用户的报表模块访问权限和数据权限信息")
@RestController
@RequestMapping("/rpt/common")
@RequiredArgsConstructor
public class ReportPermissionController {

    private final ReportPermissionService permissionService;

    /**
     * 获取当前用户的报表权限信息
     * <p>
     * 无需 @RptDataScope 注解，该接口本身用于权限查询，不需要注入数据范围。
     * </p>
     */
    @Operation(summary = "查询当前用户报表权限",
            description = "返回用户可访问的报表模块列表（ASSET/INV/OPR/FIN）、财务数据查看权限及可见项目范围")
    @GetMapping("/user-permissions")
    public R<UserPermissionVO> userPermissions() {
        Long userId = SecurityUtil.getCurrentUserId();
        String username = SecurityUtil.getCurrentUsername();

        List<Long> permittedProjectIds = permissionService.getPermittedProjectIds(userId);
        boolean hasFinViewPerm = permissionService.hasFinViewPermission(userId);
        boolean isAdmin = (permittedProjectIds == null);

        // 可访问模块：资产/招商/营运始终可访问；财务需要专属权限
        List<String> modules = new ArrayList<>(List.of("ASSET", "INV", "OPR"));
        if (hasFinViewPerm || isAdmin) {
            modules.add("FIN");
        }

        UserPermissionVO vo = new UserPermissionVO();
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setAdmin(isAdmin);
        vo.setHasFinViewPerm(hasFinViewPerm || isAdmin);
        vo.setAccessibleModules(modules);
        vo.setPermittedProjectIds(permittedProjectIds);

        return R.ok(vo);
    }
}
