package com.sub2.monitor.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sub2.monitor.common.api.ApiResponse;
import com.sub2.monitor.common.api.PageResponse;
import com.sub2.monitor.dto.AccountResponse;
import com.sub2.monitor.entity.Accounts;
import com.sub2.monitor.entity.Platform;
import com.sub2.monitor.mapper.AccountsMapper;
import com.sub2.monitor.service.AccountsService;
import com.sub2.monitor.service.PlatformService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountsService accountsService;
    private final PlatformService platformService;
    private final AccountsMapper accountsMapper;

    public AccountController(AccountsService accountsService, PlatformService platformService, AccountsMapper accountsMapper) {
        this.accountsService = accountsService;
        this.platformService = platformService;
        this.accountsMapper = accountsMapper;
    }

    @GetMapping
    public ApiResponse<PageResponse<AccountResponse>> list(@RequestParam(defaultValue = "1") long pageNo,
                                                           @RequestParam(defaultValue = "20") long pageSize,
                                                           @RequestParam(required = false) Long platformId,
                                                           @RequestParam(required = false) String keyword) {
        Page<Accounts> page = accountsMapper.selectPageOrderByPlatformBaseUrl(
                new Page<>(pageNo, pageSize),
                platformId,
                StrUtil.isBlank(keyword) ? null : keyword
        );
        List<Long> platformIds = page.getRecords().stream()
                .map(Accounts::getPlatformId)
                .distinct()
                .toList();
        Map<Long, Platform> platformMap = platformIds.isEmpty()
                ? Map.of()
                : platformService.listByIds(platformIds).stream()
                .collect(Collectors.toMap(Platform::getId, platform -> platform));

        PageResponse<AccountResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setRecords(page.getRecords().stream()
                .map(account -> toResponse(account, platformMap.get(account.getPlatformId())))
                .toList());
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<Void> save(@RequestBody Accounts request) {
        ApiResponse<Void> validation = validateRequired(request, true);
        if (validation != null) {
            return validation;
        }

        Accounts account = new Accounts();
        account.setPlatformId(request.getPlatformId());
        account.setUsername(request.getUsername());
        account.setPassword(request.getPassword());
        account.setTestModel(request.getTestModel());
        account.setCreateTime(OffsetDateTime.now());
        accountsService.save(account);
        return ApiResponse.success(null);
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestBody Accounts request) {
        if (request.getId() == null) {
            return ApiResponse.failure(400, "id is required");
        }
        ApiResponse<Void> validation = validateRequired(request, false);
        if (validation != null) {
            return validation;
        }

        Accounts account = accountsService.getById(request.getId());
        if (account == null) {
            return ApiResponse.failure(404, "account not found");
        }
        account.setPlatformId(request.getPlatformId());
        account.setUsername(request.getUsername());
        if (StrUtil.isNotBlank(request.getPassword())) {
            account.setPassword(request.getPassword());
        }
        account.setTestModel(request.getTestModel());
        accountsService.updateById(account);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        accountsService.removeById(id);
        return ApiResponse.success(null);
    }

    private ApiResponse<Void> validateRequired(Accounts request, boolean passwordRequired) {
        if (request.getPlatformId() == null) {
            return ApiResponse.failure(400, "platformId is required");
        }
        if (StrUtil.isBlank(request.getUsername())) {
            return ApiResponse.failure(400, "username is required");
        }
        if (passwordRequired && StrUtil.isBlank(request.getPassword())) {
            return ApiResponse.failure(400, "password is required");
        }
        if (platformService.getById(request.getPlatformId()) == null) {
            return ApiResponse.failure(404, "platform not found");
        }
        return null;
    }

    private AccountResponse toResponse(Accounts account, Platform platform) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setUsername(account.getUsername());
        response.setPlatformId(account.getPlatformId());
        response.setTestModel(account.getTestModel());
        response.setCreateTime(account.getCreateTime());
        if (platform != null) {
            response.setPlatformName(platform.getName());
            response.setPlatformBaseUrl(platform.getBaseUrl());
            response.setPlatformType(platform.getType());
        }
        return response;
    }
}
