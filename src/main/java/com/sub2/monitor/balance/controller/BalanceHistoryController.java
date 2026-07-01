package com.sub2.monitor.balance.controller;

import com.sub2.monitor.balance.dto.BalanceHistoryQueryRequest;
import com.sub2.monitor.balance.dto.BalanceHistoryResponse;
import com.sub2.monitor.balance.service.BalanceHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collect/balances")
@RequiredArgsConstructor
public class BalanceHistoryController {

    private final BalanceHistoryQueryService balanceHistoryQueryService;

    @GetMapping
    public BalanceHistoryResponse listBalances(BalanceHistoryQueryRequest request) {
        return balanceHistoryQueryService.listBalances(request);
    }
}

