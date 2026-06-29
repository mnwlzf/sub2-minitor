package com.sub2.monitor;

import com.sub2.monitor.collect.newApi.service.NewApiCollectService;
import com.sub2.monitor.collect.sub2api.service.Sub2CollectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Sub2MonitorApplicationTests {

    @Autowired
    private Sub2CollectService service;

    @Autowired
    private NewApiCollectService newApiCollectService;

    @Test
    void contextLoads() {
        newApiCollectService.login("https://relayai.tech");
        newApiCollectService.collectGroups("https://relayai.tech");
        newApiCollectService.collectNewApiKeys("https://relayai.tech");
    }

}
