package com.sub2.monitor.collect.service;

import com.sub2.monitor.collect.dto.BalanceHistoryQueryRequest;
import com.sub2.monitor.collect.dto.BalanceHistoryResponse;

public interface BalanceHistoryQueryService {

    BalanceHistoryResponse listBalances(BalanceHistoryQueryRequest request);
}
