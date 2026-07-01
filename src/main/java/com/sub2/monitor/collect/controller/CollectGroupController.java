package com.sub2.monitor.collect.controller;

import com.sub2.monitor.collect.dto.CollectGroupQueryRequest;
import com.sub2.monitor.collect.dto.CollectGroupResponse;
import com.sub2.monitor.collect.service.CollectGroupQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collect/groups")
@RequiredArgsConstructor
public class CollectGroupController {

    private final CollectGroupQueryService collectGroupQueryService;

    @GetMapping
    public CollectGroupResponse listGroups(CollectGroupQueryRequest request) {
        return collectGroupQueryService.listGroups(request);
    }
}
