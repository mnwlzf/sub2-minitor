package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.AccountBalanceHistory;
import com.sub2.monitor.mapper.AccountBalanceHistoryMapper;
import com.sub2.monitor.service.AccountBalanceHistoryService;
import org.springframework.stereotype.Service;

@Service
public class AccountBalanceHistoryServiceImpl extends ServiceImpl<AccountBalanceHistoryMapper, AccountBalanceHistory> implements AccountBalanceHistoryService {
}

