package com.asset.base.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商家附件保存 DTO（只保存文件元数据，文件已由 file 服务上传）
 */
@Data
public class AttachmentSaveDTO {

    /** 文件名称（必填） */
    @NotBlank(message = "文件名称不能为空")
    private String fileName;

    /** 文件地址（必填） */
    @NotBlank(message = "文件地址不能为空")
    private String fileUrl;

    /** 文件类型（如 pdf/image 等） */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;
}
