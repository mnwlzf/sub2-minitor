package com.sub2.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.dto.MailSmtpConfigRequest;
import com.sub2.monitor.dto.MailSmtpConfigResponse;
import com.sub2.monitor.entity.MailSmtpConfig;

public interface MailSmtpConfigService extends IService<MailSmtpConfig> {
    MailSmtpConfigResponse getDefaultConfig();

    MailSmtpConfigResponse saveDefaultConfig(MailSmtpConfigRequest request);

    void testConfig(MailSmtpConfigRequest request);

    String decryptPassword(MailSmtpConfig config);
}
