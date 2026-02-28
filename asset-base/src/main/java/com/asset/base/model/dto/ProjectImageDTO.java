package com.asset.base.model.dto;

import lombok.Data;

/**
 * 项目图片新增 DTO
 */
@Data
public class ProjectImageDTO {

    /** 图片访问 URL（由 asset-file 服务上传后返回） */
    private String url;

    /** 图片名称，如"外观图"、"内景图" */
    private String name;
}
