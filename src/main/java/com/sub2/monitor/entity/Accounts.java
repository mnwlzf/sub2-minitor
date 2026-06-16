package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sub2.monitor.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("accounts")
public class Accounts extends BaseEntity {
    private String username;
    private String password;
    private Long platformId;
    private OffsetDateTime createTime;
    private String testModel;
}
