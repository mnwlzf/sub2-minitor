package com.sub2.monitor.mapper.sub2api;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sub2.monitor.dto.sub2api.Sub2ApiAccountProxy;
import com.sub2.monitor.entity.sub2api.Sub2ApiAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface Sub2ApiAccountMapper extends BaseMapper<Sub2ApiAccount> {

    /**
     * 根据 NewAPI 平台 baseUrl，从 sub2api 库匹配账号及其代理配置。
     *
     * <p>注意：这个 Mapper 位于 {@code com.sub2.monitor.mapper.sub2api} 包下，
     * 会被 {@code Sub2ApiDataSourceConfig} 绑定到 sub2api 数据源。</p>
     *
     * @param baseUrl NewAPI 平台地址，对应 accounts.credentials ->> 'base_url'
     * @return 匹配到的账号代理信息；proxyId 为空时代表直连
     */
    List<Sub2ApiAccountProxy> selectAccountProxiesByBaseUrl(@Param("baseUrl") String baseUrl);
}
