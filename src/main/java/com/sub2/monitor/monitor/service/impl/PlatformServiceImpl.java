package com.sub2.monitor.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.monitor.dto.PlatformSummaryResponse;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import com.sub2.monitor.monitor.service.PlatformService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlatformServiceImpl extends ServiceImpl<PlatformMapper, Platform> implements PlatformService {

    private final AccountMapper accountMapper;

    public PlatformServiceImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

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
}
