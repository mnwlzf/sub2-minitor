package com.sub2.monitor.collect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("collect_group")
public class CollectGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("platform_id")
    private Long platformId;

    @TableField("platform_type")
    private String platformType;

    @TableField("base_url")
    private String baseUrl;

    @TableField("group_name")
    private String groupName;

    private String description;

    @TableField("rate_multiplier")
    private BigDecimal rateMultiplier;

    private String status;

    @TableField("raw_json")
    private String rawJson;

    @TableField("last_collected_at")
    private LocalDateTime lastCollectedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
