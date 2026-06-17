package com.sub2.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sub2.monitor.entity.AccountApiKeyGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountApiKeyGroupMapper extends BaseMapper<AccountApiKeyGroup> {
    void upsertBatch(@Param("items") List<AccountApiKeyGroup> items);
}
