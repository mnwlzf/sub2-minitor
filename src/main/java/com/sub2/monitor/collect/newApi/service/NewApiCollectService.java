package com.sub2.monitor.collect.newApi.service;

import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiGroupsResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiLoginRes;
import com.sub2.monitor.collect.newApi.dto.NewApiTokensResponse;

public interface NewApiCollectService {
    LoginResponse<NewApiLoginRes> login(String baseUrl);

    NewApiGroupsResponse collectGroups(String baseUrl);

    NewApiTokensResponse collectTokens(String baseUrl);
}
