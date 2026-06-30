package com.sub2.monitor.collect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collect_snapshot")
public class CollectSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("platform_id")
    private Long platformId;

    @TableField("platform_type")
    private String platformType;

    @TableField("base_url")
    private String baseUrl;

    @TableField("collect_type")
    private String collectType;

    private Integer success;

    @TableField("item_count")
    private Integer itemCount;

    private String message;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("collected_at")
    private LocalDateTime collectedAt;
}
