package com.sub2.monitor.common.base;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseEntity {
    private Long id;

    @TableField(exist = false)
    private Long createdBy;

    @TableField(exist = false)
    private Long updatedBy;

    @TableField(exist = false)
    private OffsetDateTime createdAt;

    @TableField(exist = false)
    private OffsetDateTime updatedAt;

    @TableField(exist = false)
    private OffsetDateTime deletedAt;

    private OffsetDateTime createTime;
}
