package com.sub2.monitor.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.sub2.monitor.monitor.entity.Account;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
    // 自定义 SQL 可在此添加方法，并在 XML 中实现

    List<Account> selectByBaseUrl(String baseUrl);


    List<Account> selectSub2apiAccounts(String baseUrl);


    List<Account> selectNewApiAccounts(String baseUrl);
}