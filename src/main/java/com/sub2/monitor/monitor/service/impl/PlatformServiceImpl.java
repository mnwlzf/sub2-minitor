package com.sub2.monitor.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.collect.newApi.service.NewApiCollectService;
import com.sub2.monitor.collect.sub2api.service.Sub2CollectService;
import com.sub2.monitor.monitor.dto.PlatformSummaryResponse;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import com.sub2.monitor.monitor.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformServiceImpl extends ServiceImpl<PlatformMapper, Platform> implements PlatformService {

    private final AccountMapper accountMapper;
    private final Sub2CollectService sub2CollectService;
    private final NewApiCollectService newApiCollectService;

    @Override
    public PlatformSummaryResponse listPlatformSummary(String keyword, Boolean enabled) {
        LambdaQueryWrapper<Platform> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(Platform::getPlatformName, keyword)
                    .or()
                    .like(Platform::getBaseUrl, keyword)
            );
        }
        if (enabled != null) {
            wrapper.eq(Platform::getEnabled, enabled);
        }
        wrapper.orderByDesc(Platform::getId);

        List<PlatformSummaryResponse.PlatformItem> items = list(wrapper).stream()
                .map(this::toPlatformItem)
                .toList();

        PlatformSummaryResponse.Summary summary = new PlatformSummaryResponse.Summary();
        summary.setPlatformCount(items.size());
        summary.setEnabledCount((int) items.stream().filter(item -> Boolean.TRUE.equals(item.getEnabled())).count());
        summary.setAccountCount(items.stream().mapToInt(PlatformSummaryResponse.PlatformItem::getAccountCount).sum());
        summary.setAbnormalCount(items.stream().mapToInt(PlatformSummaryResponse.PlatformItem::getAbnormalCount).sum());
        summary.setPlatformConsumption(items.stream()
                .map(PlatformSummaryResponse.PlatformItem::getPlatformConsumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setActualConsumption(items.stream()
                .map(PlatformSummaryResponse.PlatformItem::getActualConsumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        PlatformSummaryResponse response = new PlatformSummaryResponse();
        response.setItems(items);
        response.setSummary(summary);
        return response;
    }

    @Override
    public Platform createPlatform(Platform platform) {
        normalize(platform);
        save(platform);
        return getById(platform.getId());
    }

    @Override
    public Platform updatePlatform(Long id, Platform platform) {
        Platform existing = getPlatformOrThrow(id);
        existing.setPlatformName(platform.getPlatformName());
        existing.setBaseUrl(platform.getBaseUrl());
        existing.setEnabled(platform.getEnabled());
        existing.setType(platform.getType());
        normalize(existing);
        updateById(existing);
        return getById(id);
    }

    @Override
    public void deletePlatform(Long id) {
        getPlatformOrThrow(id);
        removeById(id);
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        Platform platform = getPlatformOrThrow(id);
        platform.setEnabled(enabled);
        updateById(platform);
    }

    @Override
    public void collectPlatform(Long id) {
        Platform platform = getPlatformOrThrow(id);
        String type = platform.getType() == null ? "" : platform.getType().toUpperCase();
        if ("NEWAPI".equals(type)) {
            newApiCollectService.login(platform.getBaseUrl());
            newApiCollectService.collectGroups(platform.getBaseUrl());
            newApiCollectService.collectNewApiKeys(platform.getBaseUrl());
            return;
        }
        if ("SUB2API".equals(type)) {
            sub2CollectService.login(platform.getBaseUrl());
            sub2CollectService.collectSub2AvailableGroups(platform.getBaseUrl());
            sub2CollectService.collectSub2Keys(platform.getBaseUrl());
            return;
        }
        throw new IllegalArgumentException("不支持的平台类型: " + platform.getType());
    }

    private PlatformSummaryResponse.PlatformItem toPlatformItem(Platform platform) {
        Long accountCount = accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getPlatformId, platform.getId()));

        PlatformSummaryResponse.PlatformItem item = new PlatformSummaryResponse.PlatformItem();
        item.setId(platform.getId());
        item.setPlatformName(platform.getPlatformName());
        item.setBaseUrl(platform.getBaseUrl());
        item.setEnabled(platform.getEnabled());
        item.setType(platform.getType());
        item.setAccountCount(accountCount.intValue());
        item.setTotalBalance(BigDecimal.ZERO);
        item.setPlatformConsumption(BigDecimal.ZERO);
        item.setActualConsumption(BigDecimal.ZERO);
        item.setRechargeAmount(BigDecimal.ZERO);
        item.setArrivalAmount(BigDecimal.ZERO);
        item.setAbnormalCount(0);
        item.setLastCollectedAt(null);
        return item;
    }

    private Platform getPlatformOrThrow(Long id) {
        Platform platform = getById(id);
        if (platform == null) {
            throw new IllegalArgumentException("平台不存在: " + id);
        }
        return platform;
    }

    private void normalize(Platform platform) {
        if (!StringUtils.hasText(platform.getPlatformName())) {
            throw new IllegalArgumentException("platformName 不能为空");
        }
        if (!StringUtils.hasText(platform.getBaseUrl())) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        if (!StringUtils.hasText(platform.getType())) {
            throw new IllegalArgumentException("type 不能为空");
        }
        if (platform.getEnabled() == null) {
            platform.setEnabled(true);
        }
        platform.setType(platform.getType().toUpperCase());
    }
}
