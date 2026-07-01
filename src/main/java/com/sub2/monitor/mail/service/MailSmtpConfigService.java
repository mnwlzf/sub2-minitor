package com.sub2.monitor.mail.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.mail.dto.MailSmtpConfigRequest;
import com.sub2.monitor.mail.dto.MailSmtpConfigResponse;
import com.sub2.monitor.mail.entity.MailSmtpConfig;

import java.util.List;

public interface MailSmtpConfigService extends IService<MailSmtpConfig> {

    List<MailSmtpConfigResponse> listConfigs();

    MailSmtpConfigResponse createConfig(MailSmtpConfigRequest request);

    MailSmtpConfigResponse updateConfig(Long id, MailSmtpConfigRequest request);

    void deleteConfig(Long id);

    void testConnection(MailSmtpConfigRequest request);
}
