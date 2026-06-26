package com.sub2.monitor;

import com.sub2.monitor.collect.sub2api.service.Sub2ApiCollectService;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class Sub2MonitorApplicationTests {

    @Autowired
    private Sub2ApiCollectService service;

    @Test
    void contextLoads() {
        service.login("https://codex.trovebox.online");
    }

}
