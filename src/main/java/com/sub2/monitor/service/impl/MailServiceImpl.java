package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.entity.MailNotificationScene;
import com.sub2.monitor.entity.MailRecipient;
import com.sub2.monitor.entity.MailSceneRecipient;
import com.sub2.monitor.entity.MailSendLog;
import com.sub2.monitor.entity.MailSmtpConfig;
import com.sub2.monitor.mapper.MailNotificationSceneMapper;
import com.sub2.monitor.mapper.MailRecipientMapper;
import com.sub2.monitor.mapper.MailSceneRecipientMapper;
import com.sub2.monitor.service.MailSendLogService;
import com.sub2.monitor.service.MailService;
import com.sub2.monitor.service.MailSmtpConfigService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class MailServiceImpl implements MailService {

    private final MailSmtpConfigService mailSmtpConfigService;
    private final MailNotificationSceneMapper mailNotificationSceneMapper;
    private final MailSceneRecipientMapper mailSceneRecipientMapper;
    private final MailRecipientMapper mailRecipientMapper;
    private final MailSendLogService mailSendLogService;

    public MailServiceImpl(MailSmtpConfigService mailSmtpConfigService,
                           MailNotificationSceneMapper mailNotificationSceneMapper,
                           MailSceneRecipientMapper mailSceneRecipientMapper,
                           MailRecipientMapper mailRecipientMapper,
                           MailSendLogService mailSendLogService) {
        this.mailSmtpConfigService = mailSmtpConfigService;
        this.mailNotificationSceneMapper = mailNotificationSceneMapper;
        this.mailSceneRecipientMapper = mailSceneRecipientMapper;
        this.mailRecipientMapper = mailRecipientMapper;
        this.mailSendLogService = mailSendLogService;
    }

    @Override
    public void sendByScene(String sceneKey, String subject, String content) {
        MailSendLog log = new MailSendLog();
        log.setSceneKey(sceneKey);
        log.setSubject(subject);
        log.setCreateTime(OffsetDateTime.now());
        try {
            MailNotificationScene scene = mailNotificationSceneMapper.selectOne(new LambdaQueryWrapper<MailNotificationScene>()
                    .eq(MailNotificationScene::getSceneKey, sceneKey));
            if (scene == null || !Boolean.TRUE.equals(scene.getIsEnabled())) {
                throw new IllegalStateException("通知场景未启用或不存在: " + sceneKey);
            }
            MailSmtpConfig smtpConfig = mailSmtpConfigService.getOne(new LambdaQueryWrapper<MailSmtpConfig>()
                    .eq(MailSmtpConfig::getIsDefault, true)
                    .eq(MailSmtpConfig::getIsEnabled, true)
                    .last("limit 1"));
            if (smtpConfig == null) {
                throw new IllegalStateException("未配置启用的默认 SMTP");
            }

            RecipientGroups groups = loadRecipients(sceneKey);
            log.setToEmails(String.join(",", groups.to()));
            log.setCcEmails(String.join(",", groups.cc()));
            log.setBccEmails(String.join(",", groups.bcc()));
            if (groups.to().isEmpty() && groups.cc().isEmpty() && groups.bcc().isEmpty()) {
                throw new IllegalStateException("通知场景未配置收件人: " + sceneKey);
            }

            JavaMailSenderImpl sender = buildSender(smtpConfig);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(smtpConfig.getFromEmail(), smtpConfig.getFromName());
            helper.setSubject(subject);
            helper.setText(content, true);
            if (!groups.to().isEmpty()) {
                helper.setTo(groups.to().toArray(String[]::new));
            }
            if (!groups.cc().isEmpty()) {
                helper.setCc(groups.cc().toArray(String[]::new));
            }
            if (!groups.bcc().isEmpty()) {
                helper.setBcc(groups.bcc().toArray(String[]::new));
            }
            sender.send(message);
            log.setStatus("SUCCESS");
            log.setSendTime(OffsetDateTime.now());
            mailSendLogService.save(log);
        } catch (Exception exception) {
            log.setStatus("FAILED");
            log.setErrorMessage(exception.getMessage());
            mailSendLogService.save(log);
            throw new IllegalStateException("邮件发送失败: " + exception.getMessage(), exception);
        }
    }

    private JavaMailSenderImpl buildSender(MailSmtpConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(mailSmtpConfigService.decryptPassword(config));
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        if (Boolean.TRUE.equals(config.getUseTls())) {
            properties.put("mail.smtp.starttls.enable", "true");
        }
        if (Boolean.TRUE.equals(config.getUseSsl())) {
            properties.put("mail.smtp.ssl.enable", "true");
        }
        return sender;
    }

    private RecipientGroups loadRecipients(String sceneKey) {
        List<MailSceneRecipient> relations = mailSceneRecipientMapper.selectList(new LambdaQueryWrapper<MailSceneRecipient>()
                .eq(MailSceneRecipient::getSceneKey, sceneKey));
        List<Long> recipientIds = relations.stream()
                .map(MailSceneRecipient::getRecipientId)
                .distinct()
                .toList();
        Map<Long, MailRecipient> recipientMap = recipientIds.isEmpty()
                ? Map.of()
                : mailRecipientMapper.selectBatchIds(recipientIds)
                .stream()
                .filter(recipient -> Boolean.TRUE.equals(recipient.getIsEnabled()))
                .collect(Collectors.toMap(MailRecipient::getId, recipient -> recipient));
        List<String> to = emailsByType(relations, recipientMap, "TO");
        List<String> cc = emailsByType(relations, recipientMap, "CC");
        List<String> bcc = emailsByType(relations, recipientMap, "BCC");
        return new RecipientGroups(to, cc, bcc);
    }

    private List<String> emailsByType(List<MailSceneRecipient> relations,
                                      Map<Long, MailRecipient> recipientMap,
                                      String recipientType) {
        return relations.stream()
                .filter(relation -> recipientType.equalsIgnoreCase(relation.getRecipientType()))
                .map(relation -> recipientMap.get(relation.getRecipientId()))
                .filter(recipient -> recipient != null && recipient.getEmail() != null && !recipient.getEmail().isBlank())
                .map(MailRecipient::getEmail)
                .distinct()
                .toList();
    }

    private record RecipientGroups(List<String> to, List<String> cc, List<String> bcc) {
    }
}
