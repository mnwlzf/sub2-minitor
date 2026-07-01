package com.sub2.monitor.account.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.account.dto.AccountRequest;
import com.sub2.monitor.account.dto.AccountResponse;
import com.sub2.monitor.monitor.entity.Account;

import java.util.List;

public interface AccountService extends IService<Account> {

    List<AccountResponse> listAccounts(Long platformId, String keyword);

    AccountResponse createAccount(AccountRequest request);

    AccountResponse updateAccount(Long id, AccountRequest request);

    void deleteAccount(Long id);
}

