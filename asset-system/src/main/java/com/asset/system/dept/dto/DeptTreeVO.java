package com.asset.system.dept.dto;

import lombok.Data;

import java.util.List;

/** 部门树节点 VO */
@Data
public class DeptTreeVO {
    private Long id;
    private Long parentId;
    private String deptName;
    private String deptCode;
    private Integer sortOrder;
    private String leader;
    private Integer status;
    private List<DeptTreeVO> children;
}
