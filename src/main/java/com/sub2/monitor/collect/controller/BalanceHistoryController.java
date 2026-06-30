package com.sub2.monitor.collect.controller;

import com.sub2.monitor.collect.dto.BalanceHistoryResponse;
import com.sub2.monitor.collect.service.BalanceHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collect/balances")
@RequiredArgsConstructor
public class BalanceHistoryController {

    private final BalanceHistoryQueryService balanceHistoryQueryService;

    @GetMapping
    public BalanceHistoryResponse listBalances(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled
    ) {
        return balanceHistoryQueryService.listBalances(keyword, enabled);
    }
}
