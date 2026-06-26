package com.sub2.monitor.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("accounts")
public class Account {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;

    @TableField("platform_id")
    private Long platformId;

    @TableField("platform_name")
    private String platformName;

    @TableField("password")
    private String password;


    @TableField("test_model")
    private String testModel;

    @TableField("is_collect")
    private Boolean isCollect;
}