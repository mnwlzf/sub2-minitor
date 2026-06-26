package com.sub2.monitor.collect.sub2api.service;

import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2AvailableGroupsResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2LoginRes;

public interface Sub2CollectService {


    LoginResponse<Sub2LoginRes> login(String base_url);


    Sub2AvailableGroupsResponse collectSub2AvailableGroups(String baseUrl);
}
