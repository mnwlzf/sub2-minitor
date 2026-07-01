package com.sub2.monitor.mail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.mail.dto.MailSmtpConfigRequest;
import com.sub2.monitor.mail.dto.MailSmtpConfigResponse;
import com.sub2.monitor.mail.entity.MailSmtpConfig;
import com.sub2.monitor.mail.mapper.MailSmtpConfigMapper;
import com.sub2.monitor.mail.service.MailSmtpConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class MailSmtpConfigServiceImpl extends ServiceImpl<MailSmtpConfigMapper, MailSmtpConfig> implements MailSmtpConfigService {

    @Override
    public List<MailSmtpConfigResponse> listConfigs() {
        return list(new LambdaQueryWrapper<MailSmtpConfig>().orderByDesc(MailSmtpConfig::getIsDefault).orderByDesc(MailSmtpConfig::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MailSmtpConfigResponse createConfig(MailSmtpConfigRequest request) {
        MailSmtpConfig config = new MailSmtpConfig();
        applyRequest(config, request);
        save(config);
        syncDefaultFlag(config);
        return toResponse(getById(config.getId()));
    }

    @Override
    @Transactional
    public MailSmtpConfigResponse updateConfig(Long id, MailSmtpConfigRequest request) {
        MailSmtpConfig config = getConfigOrThrow(id);
        applyRequest(config, request);
        updateById(config);
        syncDefaultFlag(config);
        return toResponse(getById(id));
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        getConfigOrThrow(id);
        removeById(id);
    }

    @Override
    public void testConnection(MailSmtpConfigRequest request) {
        JavaMailSenderImpl sender = buildSender(request);
        try {
            sender.testConnection();
        } catch (Exception e) {
            throw new IllegalStateException("SMTP 连接测试失败: " + e.getMessage(), e);
        }
    }

    private void applyRequest(MailSmtpConfig config, MailSmtpConfigRequest request) {
        if (!StringUtils.hasText(request.getConfigName())) {
            throw new IllegalArgumentException("configName 不能为空");
        }
        if (!StringUtils.hasText(request.getHost())) {
            throw new IllegalArgumentException("host 不能为空");
        }
        if (request.getPort() == null) {
            throw new IllegalArgumentException("port 不能为空");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("username 不能为空");
        }
        if (!StringUtils.hasText(request.getFromEmail())) {
            throw new IllegalArgumentException("fromEmail 不能为空");
        }
        config.setConfigName(request.getConfigName().trim());
        config.setHost(request.getHost().trim());
        config.setPort(request.getPort());
        config.setUsername(request.getUsername().trim());
        config.setPassword(StringUtils.hasText(request.getPassword()) ? request.getPassword().trim() : null);
        config.setFromEmail(request.getFromEmail().trim());
        config.setFromName(trimToNull(request.getFromName()));
        config.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        config.setUseTls(request.getUseTls() == null ? 0 : request.getUseTls());
        config.setUseSsl(request.getUseSsl() == null ? 1 : request.getUseSsl());
        config.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        config.setRemark(trimToNull(request.getRemark()));
    }

    private void syncDefaultFlag(MailSmtpConfig config) {
        if (!Integer.valueOf(1).equals(config.getIsDefault())) {
            return;
        }
        list(new LambdaQueryWrapper<MailSmtpConfig>()
                .ne(MailSmtpConfig::getId, config.getId())
                .eq(MailSmtpConfig::getIsDefault, 1))
                .forEach(item -> {
                    item.setIsDefault(0);
                    updateById(item);
                });
    }

    private MailSmtpConfig getConfigOrThrow(Long id) {
        MailSmtpConfig config = getById(id);
        if (config == null) {
            throw new IllegalArgumentException("SMTP配置不存在: " + id);
        }
        return config;
    }

    private MailSmtpConfigResponse toResponse(MailSmtpConfig config) {
        MailSmtpConfigResponse response = new MailSmtpConfigResponse();
        response.setId(config.getId());
        response.setConfigName(config.getConfigName());
        response.setHost(config.getHost());
        response.setPort(config.getPort());
        response.setUsername(config.getUsername());
        response.setPassword(config.getPassword());
        response.setFromEmail(config.getFromEmail());
        response.setFromName(config.getFromName());
        response.setEnabled(config.getEnabled());
        response.setUseTls(config.getUseTls());
        response.setUseSsl(config.getUseSsl());
        response.setIsDefault(config.getIsDefault());
        response.setRemark(config.getRemark());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        return response;
    }

    private JavaMailSenderImpl buildSender(MailSmtpConfigRequest request) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(request.getHost());
        sender.setPort(request.getPort());
        sender.setUsername(request.getUsername());
        sender.setPassword(request.getPassword());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        boolean useTls = Integer.valueOf(1).equals(request.getUseTls());
        boolean useSsl = Integer.valueOf(1).equals(request.getUseSsl());
        props.put("mail.smtp.starttls.enable", String.valueOf(useTls));
        props.put("mail.smtp.ssl.enable", String.valueOf(useSsl));
        return sender;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
