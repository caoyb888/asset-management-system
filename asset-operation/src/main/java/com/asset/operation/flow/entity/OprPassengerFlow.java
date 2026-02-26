package com.asset.operation.flow.entity;

import com.asset.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/** 客流填报表（支持项目/楼栋/楼层三级维度）- 对应 opr_passenger_flow */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opr_passenger_flow")
public class OprPassengerFlow extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 项目ID */
    private Long projectId;
    /** 楼栋ID（可选，项目整体录入时为空） */
    private Long buildingId;
    /** 楼层ID（可选） */
    private Long floorId;
    /** 填报日期 */
    private LocalDate reportDate;
    /** 客流人数 */
    private Integer flowCount;
    /** 数据来源（1手动/2导入/3设备对接） */
    private Integer sourceType;
}
