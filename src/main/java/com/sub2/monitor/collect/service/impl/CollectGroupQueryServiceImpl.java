package com.sub2.monitor.collect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.collect.dto.CollectGroupQueryRequest;
import com.sub2.monitor.collect.dto.CollectGroupResponse;
import com.sub2.monitor.collect.entity.CollectGroup;
import com.sub2.monitor.collect.mapper.CollectGroupMapper;
import com.sub2.monitor.collect.service.CollectGroupQueryService;
import com.sub2.monitor.monitor.entity.Platform;
import com.sub2.monitor.monitor.mapper.PlatformMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectGroupQueryServiceImpl implements CollectGroupQueryService {

    private static final BigDecimal DEFAULT_RATIO = BigDecimal.ONE;

    private final PlatformMapper platformMapper;
    private final CollectGroupMapper collectGroupMapper;

    @Override
    public CollectGroupResponse listGroups(CollectGroupQueryRequest request) {
        CollectGroupQueryRequest query = request == null ? new CollectGroupQueryRequest() : request;
        LambdaQueryWrapper<Platform> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(platformQuery -> platformQuery
                    .like(Platform::getPlatformName, query.getKeyword())
                    .or()
                    .like(Platform::getBaseUrl, query.getKeyword())
            );
        }
        if (query.getEnabled() != null) {
            wrapper.eq(Platform::getEnabled, query.getEnabled());
        }
        wrapper.orderByDesc(Platform::getId);

        List<CollectGroupResponse.PlatformGroupItem> items = platformMapper.selectList(wrapper).stream()
                .map(this::toPlatformGroupItem)
                .toList();

        CollectGroupResponse response = new CollectGroupResponse();
        response.setItems(items);
        response.setTotal(items.size());
        return response;
    }

    private CollectGroupResponse.PlatformGroupItem toPlatformGroupItem(Platform platform) {
        List<CollectGroup> groups = collectGroupMapper.selectList(new LambdaQueryWrapper<CollectGroup>()
                .eq(CollectGroup::getPlatformId, platform.getId())
                .orderByAsc(CollectGroup::getGroupName));
        LocalDateTime lastCollectedAt = groups.stream()
                .map(CollectGroup::getLastCollectedAt)
                .filter(time -> time != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        CollectGroupResponse.PlatformGroupItem item = new CollectGroupResponse.PlatformGroupItem();
        item.setPlatformId(platform.getId());
        item.setPlatformName(platform.getPlatformName());
        item.setPlatformType(platform.getType());
        item.setBaseUrl(platform.getBaseUrl());
        item.setEnabled(platform.getEnabled());
        item.setGroupCount(groups.size());
        item.setRechargeRatio(DEFAULT_RATIO);
        item.setDiscountRatio(DEFAULT_RATIO);
        item.setLastCollectedAt(lastCollectedAt);
        item.setGroups(groups.stream().map(this::toGroupItem).toList());
        return item;
    }

    private CollectGroupResponse.GroupItem toGroupItem(CollectGroup group) {
        CollectGroupResponse.GroupItem item = new CollectGroupResponse.GroupItem();
        item.setId(group.getId());
        item.setGroupName(group.getGroupName());
        item.setDescription(group.getDescription());
        item.setPlatformRate(group.getRateMultiplier());
        item.setActualRate(group.getRateMultiplier());
        item.setStatus(group.getStatus());
        item.setLastCollectedAt(group.getLastCollectedAt());
        return item;
    }
}
