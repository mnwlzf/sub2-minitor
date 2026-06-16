package com.sub2.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sub2.monitor.entity.Accounts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountsMapper extends BaseMapper<Accounts> {

    List<Accounts> selectByNames(@Param("names") List<String> names, @Param("platformId") Long platformId);
}
