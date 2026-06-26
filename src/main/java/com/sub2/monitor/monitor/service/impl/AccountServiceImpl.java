package com.sub2.monitor.monitor.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import com.sub2.monitor.monitor.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {
    // 无需编写基础 CRUD 方法，MP 已提供

    @Autowired
    private AccountMapper accountMapper;


    @Autowired
    private PlatformMapper platformMapper;

}
