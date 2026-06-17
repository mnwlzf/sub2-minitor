package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.dto.MailSmtpConfigRequest;
import com.sub2.monitor.dto.MailSmtpConfigResponse;
import com.sub2.monitor.entity.MailSmtpConfig;
import com.sub2.monitor.mapper.MailSmtpConfigMapper;
import com.sub2.monitor.service.MailSmtpConfigService;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;

@Service
public class MailSmtpConfigServiceImpl extends ServiceImpl<MailSmtpConfigMapper, MailSmtpConfig> implements MailSmtpConfigService {

    @Value("${app.mail.secret:${spring.application.name:sub2-monitor}}")
    private String mailSecret;

    @Override
    public MailSmtpConfigResponse getDefaultConfig() {
        MailSmtpConfig config = loadDefaultConfig();
        return config == null ? null : toResponse(config);
    }

    @Override
    public MailSmtpConfigResponse saveDefaultConfig(MailSmtpConfigRequest request) {
        validateRequest(request, false);
        MailSmtpConfig config = loadDefaultConfig();
        OffsetDateTime now = OffsetDateTime.now();
        if (config == null) {
            config = new MailSmtpConfig();
            config.setCreateTime(now);
            config.setIsDefault(true);
        }
        config.setConfigName(defaultString(request.getConfigName(), "默认 SMTP"));
        config.setHost(request.getHost());
        config.setPort(request.getPort());
        config.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            config.setPasswordEncrypted(encrypt(request.getPassword()));
        }
        config.setFromEmail(request.getFromEmail());
        config.setFromName(request.getFromName());
        config.setUseTls(Optional.ofNullable(request.getUseTls()).orElse(true));
        config.setUseSsl(Optional.ofNullable(request.getUseSsl()).orElse(false));
        config.setIsEnabled(Optional.ofNullable(request.getIsEnabled()).orElse(true));
        config.setIsDefault(true);
        config.setUpdateTime(now);
        saveOrUpdate(config);
        return toResponse(config);
    }

    @Override
    public void testConfig(MailSmtpConfigRequest request) {
        validateRequest(request, true);
        String password = request.getPassword();
        if (password == null || password.isBlank()) {
            MailSmtpConfig saved = loadDefaultConfig();
            if (saved == null || saved.getPasswordEncrypted() == null || saved.getPasswordEncrypted().isBlank()) {
                throw new IllegalArgumentException("SMTP 密码不能为空");
            }
            password = decryptPassword(saved);
        }
        testConnection(
                request.getHost(),
                request.getPort(),
                request.getUsername(),
                password,
                Boolean.TRUE.equals(request.getUseTls()),
                Boolean.TRUE.equals(request.getUseSsl())
        );
    }

    @Override
    public String decryptPassword(MailSmtpConfig config) {
        if (config == null || config.getPasswordEncrypted() == null || config.getPasswordEncrypted().isBlank()) {
            return "";
        }
        return decrypt(config.getPasswordEncrypted());
    }

    private MailSmtpConfig loadDefaultConfig() {
        return getOne(new LambdaQueryWrapper<MailSmtpConfig>()
                .eq(MailSmtpConfig::getIsDefault, true)
                .last("limit 1"));
    }

    private void validateRequest(MailSmtpConfigRequest request, boolean allowSavedPassword) {
        if (request.getHost() == null || request.getHost().isBlank()) {
            throw new IllegalArgumentException("SMTP 主机不能为空");
        }
        if (request.getPort() == null || request.getPort() <= 0) {
            throw new IllegalArgumentException("SMTP 端口无效");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("SMTP 用户名不能为空");
        }
        if ((request.getPassword() == null || request.getPassword().isBlank()) && !allowSavedPassword) {
            MailSmtpConfig saved = loadDefaultConfig();
            if (saved == null || saved.getPasswordEncrypted() == null || saved.getPasswordEncrypted().isBlank()) {
                throw new IllegalArgumentException("SMTP 密码不能为空");
            }
        }
        if (request.getFromEmail() == null || request.getFromEmail().isBlank()) {
            throw new IllegalArgumentException("发件人邮箱不能为空");
        }
        if (Boolean.TRUE.equals(request.getUseTls()) && Boolean.TRUE.equals(request.getUseSsl())) {
            throw new IllegalArgumentException("TLS 和 SSL 不能同时启用");
        }
    }

    private void testConnection(String host, Integer port, String username, String password, boolean useTls, boolean useSsl) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        if (useTls) {
            properties.put("mail.smtp.starttls.enable", "true");
        }
        if (useSsl) {
            properties.put("mail.smtp.ssl.enable", "true");
        }
        try {
            Session session = Session.getInstance(properties);
            Transport transport = session.getTransport("smtp");
            transport.connect(host, port, username, password);
            transport.close();
        } catch (Exception exception) {
            throw new IllegalArgumentException("SMTP 连接失败: " + exception.getMessage(), exception);
        }
    }

    private MailSmtpConfigResponse toResponse(MailSmtpConfig config) {
        MailSmtpConfigResponse response = new MailSmtpConfigResponse();
        response.setId(config.getId());
        response.setConfigName(config.getConfigName());
        response.setHost(config.getHost());
        response.setPort(config.getPort());
        response.setUsername(config.getUsername());
        response.setPasswordConfigured(config.getPasswordEncrypted() != null && !config.getPasswordEncrypted().isBlank());
        response.setFromEmail(config.getFromEmail());
        response.setFromName(config.getFromName());
        response.setUseTls(config.getUseTls());
        response.setUseSsl(config.getUseSsl());
        response.setIsEnabled(config.getIsEnabled());
        response.setIsDefault(config.getIsDefault());
        response.setCreateTime(config.getCreateTime());
        response.setUpdateTime(config.getUpdateTime());
        return response;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String encrypt(String rawValue) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey());
            return Base64.getEncoder().encodeToString(cipher.doFinal(rawValue.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SMTP 密码加密失败", exception);
        }
    }

    private String decrypt(String encryptedValue) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey());
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("SMTP 密码解密失败", exception);
        }
    }

    private SecretKeySpec secretKey() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(mailSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(digest, 16), "AES");
    }
}
