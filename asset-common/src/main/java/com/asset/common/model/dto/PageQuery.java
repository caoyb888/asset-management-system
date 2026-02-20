package com.asset.common.model.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class PageQuery {
    @Min(1)
    private int pageNum = 1;
    @Min(1) @Max(500)
    private int pageSize = 20;
    private String orderBy;
    private String orderDirection = "asc";
}
