package com.sub2.monitor;

import com.sub2.monitor.collect.sub2api.service.Sub2CollectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Sub2MonitorApplicationTests {

    @Autowired
    private Sub2CollectService service;

    @Test
    void contextLoads() {
        service.login("https://codex.trovebox.online");
        service.collectSub2AvailableGroups("https://codex.trovebox.online");
        service.collectSub2Keys("https://codex.trovebox.online");
    }

}
