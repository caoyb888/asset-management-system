package com.asset.system.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 用户岗位关联表 sys_user_post */
@Data
@TableName("sys_user_post")
public class SysUserPost {
    private Long userId;
    private Long postId;
}
