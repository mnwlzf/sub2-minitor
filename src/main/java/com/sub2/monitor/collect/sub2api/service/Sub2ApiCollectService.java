package com.sub2.monitor.collect.sub2api.service;

import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2ApiLoginRequest;
import com.sub2.monitor.collect.sub2api.dto.Sub2ApiLoginRes;

public interface Sub2ApiCollectService {


    LoginResponse<Sub2ApiLoginRes> login(String base_url);
}
