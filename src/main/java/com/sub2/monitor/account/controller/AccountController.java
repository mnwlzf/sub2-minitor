package com.sub2.monitor.account.controller;

import com.sub2.monitor.account.dto.AccountQueryRequest;
import com.sub2.monitor.account.dto.AccountRequest;
import com.sub2.monitor.account.dto.AccountResponse;
import com.sub2.monitor.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitor/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> listAccounts(AccountQueryRequest request) {
        return accountService.listAccounts(request);
    }

    @PostMapping
    public AccountResponse createAccount(@RequestBody AccountRequest request) {
        return accountService.createAccount(request);
    }

    @PutMapping("/{id}")
    public AccountResponse updateAccount(@PathVariable Long id, @RequestBody AccountRequest request) {
        return accountService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
    }
}

