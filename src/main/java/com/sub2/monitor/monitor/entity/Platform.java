package com.sub2.monitor.monitor.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("platform")
public class Platform {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 注意数据库列名为 plaform_name（拼写），通过 @TableField 映射
    @TableField("plaform_name")
    private String platformName;

    @TableField("base_url")
    private String baseUrl;

    private Boolean enabled;
    private String type;
}