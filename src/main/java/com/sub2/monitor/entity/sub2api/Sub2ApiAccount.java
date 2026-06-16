package com.sub2.monitor.entity.sub2api;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("accounts")
public class Sub2ApiAccount {
    private Long id;
    private String name;
    private Long proxyId;
}
