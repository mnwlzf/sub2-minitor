package com.sub2.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sub2.monitor.entity.Accounts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountsMapper extends BaseMapper<Accounts> {

    List<Accounts> selectByNames(@Param("names") List<String> names, @Param("platformId") Long platformId);

    Page<Accounts> selectPageOrderByPlatformBaseUrl(Page<Accounts> page,
                                                    @Param("platformId") Long platformId,
                                                    @Param("keyword") String keyword);
}
