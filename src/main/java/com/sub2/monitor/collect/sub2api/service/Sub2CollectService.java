package com.sub2.monitor.collect.sub2api.service;

import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2AvailableGroupsResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2KeysResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2LoginRes;
import com.sub2.monitor.collect.sub2api.dto.Sub2UsageStatsResponse;

public interface Sub2CollectService {


    LoginResponse<Sub2LoginRes> login(String base_url);


    Sub2AvailableGroupsResponse collectSub2AvailableGroups(String baseUrl);


    Sub2KeysResponse collectSub2Keys(String baseUrl);

    Sub2UsageStatsResponse collectUsageStats(String baseUrl);
}
