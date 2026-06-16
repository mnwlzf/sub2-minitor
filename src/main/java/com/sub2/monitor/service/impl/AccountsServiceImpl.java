package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.Accounts;
import com.sub2.monitor.mapper.AccountsMapper;
import com.sub2.monitor.service.AccountsService;
import org.springframework.stereotype.Service;

@Service
public class AccountsServiceImpl extends ServiceImpl<AccountsMapper, Accounts> implements AccountsService {
}

