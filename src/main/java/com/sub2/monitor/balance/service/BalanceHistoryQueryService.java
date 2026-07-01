package com.sub2.monitor.balance.service;

import com.sub2.monitor.balance.dto.BalanceHistoryQueryRequest;
import com.sub2.monitor.balance.dto.BalanceHistoryResponse;

public interface BalanceHistoryQueryService {

    BalanceHistoryResponse listBalances(BalanceHistoryQueryRequest request);
}

