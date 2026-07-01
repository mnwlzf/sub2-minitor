package com.sub2.monitor.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.account.dto.AccountRequest;
import com.sub2.monitor.account.dto.AccountResponse;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import com.sub2.monitor.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    private final PlatformMapper platformMapper;

    @Override
    public List<AccountResponse> listAccounts(Long platformId, String keyword) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        if (platformId != null) {
            wrapper.eq(Account::getPlatformId, platformId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(Account::getUsername, keyword)
                    .or()
                    .like(Account::getEmail, keyword));
        }
        wrapper.orderByDesc(Account::getId);

        List<Account> accounts = list(wrapper);
        Map<Long, Platform> platformMap = platformMapper.selectList(null).stream()
                .collect(Collectors.toMap(Platform::getId, Function.identity(), (left, right) -> left));
        return accounts.stream()
                .map(account -> toResponse(account, platformMap.get(account.getPlatformId())))
                .toList();
    }

    @Override
    public AccountResponse createAccount(AccountRequest request) {
        Platform platform = getPlatformOrThrow(request.getPlatformId());
        Account account = new Account();
        applyRequest(account, request, platform, true);
        save(account);
        return toResponse(getById(account.getId()), platform);
    }

    @Override
    public AccountResponse updateAccount(Long id, AccountRequest request) {
        Account account = getAccountOrThrow(id);
        Platform platform = getPlatformOrThrow(request.getPlatformId());
        applyRequest(account, request, platform, false);
        updateById(account);
        return toResponse(getById(id), platform);
    }

    @Override
    public void deleteAccount(Long id) {
        getAccountOrThrow(id);
        removeById(id);
    }

    private void applyRequest(Account account, AccountRequest request, Platform platform, boolean creating) {
        if (!StringUtils.hasText(request.getUsername()) && !StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("账号或邮箱不能为空");
        }
        if (creating && !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }

        account.setPlatformId(platform.getId());
        account.setPlatformName(platform.getPlatformName());
        account.setUsername(trimToNull(request.getUsername()));
        account.setEmail(trimToNull(request.getEmail()));
        account.setTestModel(trimToNull(request.getTestModel()));
        account.setIsCollect(request.getIsCollect() == null || request.getIsCollect());
        if (StringUtils.hasText(request.getPassword())) {
            account.setPassword(request.getPassword().trim());
        }
    }

    private AccountResponse toResponse(Account account, Platform platform) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setUsername(account.getUsername());
        response.setEmail(account.getEmail());
        response.setPlatformId(account.getPlatformId());
        response.setPlatformName(platform != null ? platform.getPlatformName() : account.getPlatformName());
        response.setPlatformType(platform != null ? platform.getType() : null);
        response.setTestModel(account.getTestModel());
        response.setIsCollect(account.getIsCollect());
        return response;
    }

    private Account getAccountOrThrow(Long id) {
        Account account = getById(id);
        if (account == null) {
            throw new IllegalArgumentException("账号不存在: " + id);
        }
        return account;
    }

    private Platform getPlatformOrThrow(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("platformId 不能为空");
        }
        Platform platform = platformMapper.selectById(id);
        if (platform == null) {
            throw new IllegalArgumentException("平台不存在: " + id);
        }
        return platform;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

}

