package com.sub2.monitor.collect.newApi.service;

import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiLoginRes;

public interface NewApiCollectService {
    LoginResponse<NewApiLoginRes> login(String baseUrl);
}
