package com.sub2.monitor.collect.service;

import com.sub2.monitor.collect.dto.CollectGroupQueryRequest;
import com.sub2.monitor.collect.dto.CollectGroupResponse;

public interface CollectGroupQueryService {

    CollectGroupResponse listGroups(CollectGroupQueryRequest request);
}
