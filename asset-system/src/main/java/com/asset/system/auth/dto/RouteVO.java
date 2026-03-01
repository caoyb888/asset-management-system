package com.asset.system.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 前端动态路由 VO
 */
@Data
@Builder
public class RouteVO {

    /** 路由路径 */
    private String path;

    /** 路由名称 */
    private String name;

    /** 组件路径（前端用）*/
    private String component;

    /** 路由元信息（title、icon） */
    private Map<String, Object> meta;

    /** 子路由 */
    private List<RouteVO> children;
}
