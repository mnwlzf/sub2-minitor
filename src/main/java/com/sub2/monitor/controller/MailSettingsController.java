package com.sub2.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.common.api.ApiResponse;
import com.sub2.monitor.dto.MailRecipientRequest;
import com.sub2.monitor.dto.MailSceneRecipientRequest;
import com.sub2.monitor.dto.MailSceneRequest;
import com.sub2.monitor.dto.MailSceneResponse;
import com.sub2.monitor.dto.MailSendRequest;
import com.sub2.monitor.dto.MailSmtpConfigRequest;
import com.sub2.monitor.dto.MailSmtpConfigResponse;
import com.sub2.monitor.entity.MailRecipient;
import com.sub2.monitor.entity.MailNotificationScene;
import com.sub2.monitor.service.MailNotificationSceneService;
import com.sub2.monitor.service.MailRecipientService;
import com.sub2.monitor.service.MailService;
import com.sub2.monitor.service.MailSmtpConfigService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/mail-settings")
public class MailSettingsController {

    private final MailSmtpConfigService mailSmtpConfigService;
    private final MailRecipientService mailRecipientService;
    private final MailNotificationSceneService mailNotificationSceneService;
    private final MailService mailService;

    public MailSettingsController(MailSmtpConfigService mailSmtpConfigService,
                                  MailRecipientService mailRecipientService,
                                  MailNotificationSceneService mailNotificationSceneService,
                                  MailService mailService) {
        this.mailSmtpConfigService = mailSmtpConfigService;
        this.mailRecipientService = mailRecipientService;
        this.mailNotificationSceneService = mailNotificationSceneService;
        this.mailService = mailService;
    }

    @GetMapping("/smtp")
    public ApiResponse<MailSmtpConfigResponse> getSmtpConfig() {
        return ApiResponse.success(mailSmtpConfigService.getDefaultConfig());
    }

    @PutMapping("/smtp")
    public ApiResponse<MailSmtpConfigResponse> saveSmtpConfig(@RequestBody MailSmtpConfigRequest request) {
        try {
            return ApiResponse.success(mailSmtpConfigService.saveDefaultConfig(request));
        } catch (IllegalArgumentException exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    @PostMapping("/smtp/test")
    public ApiResponse<Void> testSmtpConfig(@RequestBody MailSmtpConfigRequest request) {
        try {
            mailSmtpConfigService.testConfig(request);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    @GetMapping("/recipients")
    public ApiResponse<List<MailRecipient>> listRecipients() {
        return ApiResponse.success(mailRecipientService.list(new LambdaQueryWrapper<MailRecipient>()
                .orderByDesc(MailRecipient::getCreateTime)));
    }

    @PostMapping("/recipients")
    public ApiResponse<MailRecipient> saveRecipient(@RequestBody MailRecipientRequest request) {
        try {
            MailRecipient recipient = toRecipient(request);
            recipient.setCreateTime(OffsetDateTime.now());
            recipient.setUpdateTime(OffsetDateTime.now());
            mailRecipientService.save(recipient);
            return ApiResponse.success(recipient);
        } catch (IllegalArgumentException exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    @PutMapping("/recipients")
    public ApiResponse<MailRecipient> updateRecipient(@RequestBody MailRecipientRequest request) {
        if (request.getId() == null) {
            return ApiResponse.failure(400, "id is required");
        }
        MailRecipient recipient = mailRecipientService.getById(request.getId());
        if (recipient == null) {
            return ApiResponse.failure(404, "recipient not found");
        }
        try {
            validateRecipient(request);
            recipient.setEmail(request.getEmail());
            recipient.setName(request.getName());
            recipient.setIsEnabled(Optional.ofNullable(request.getIsEnabled()).orElse(true));
            recipient.setUpdateTime(OffsetDateTime.now());
            mailRecipientService.updateById(recipient);
            return ApiResponse.success(recipient);
        } catch (IllegalArgumentException exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/recipients/{id}")
    public ApiResponse<Void> deleteRecipient(@PathVariable Long id) {
        mailRecipientService.removeById(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/scenes")
    public ApiResponse<List<MailSceneResponse>> listScenes() {
        return ApiResponse.success(mailNotificationSceneService.listScenes());
    }

    @PostMapping("/scenes")
    public ApiResponse<MailNotificationScene> saveScene(@RequestBody MailSceneRequest request) {
        try {
            return ApiResponse.success(mailNotificationSceneService.saveScene(request));
        } catch (IllegalArgumentException exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    @PutMapping("/scenes")
    public ApiResponse<MailNotificationScene> updateScene(@RequestBody MailSceneRequest request) {
        try {
            return ApiResponse.success(mailNotificationSceneService.updateScene(request));
        } catch (IllegalArgumentException exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/scenes/{id}")
    public ApiResponse<Void> deleteScene(@PathVariable Long id) {
        mailNotificationSceneService.deleteScene(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/scenes/recipients")
    public ApiResponse<Void> addSceneRecipient(@RequestBody MailSceneRecipientRequest request) {
        try {
            mailNotificationSceneService.addRecipient(request);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/scenes/recipients/{relationId}")
    public ApiResponse<Void> removeSceneRecipient(@PathVariable Long relationId) {
        mailNotificationSceneService.removeRecipient(relationId);
        return ApiResponse.success(null);
    }

    @PostMapping("/send-test")
    public ApiResponse<Void> sendTest(@RequestBody MailSendRequest request) {
        try {
            mailService.sendByScene(
                    Optional.ofNullable(request.getSceneKey()).orElse("collect_failed"),
                    Optional.ofNullable(request.getSubject()).orElse("Sub2 Monitor 邮件测试"),
                    Optional.ofNullable(request.getContent()).orElse("<p>这是一封来自 Sub2 Monitor 的测试邮件。</p>")
            );
            return ApiResponse.success(null);
        } catch (IllegalStateException exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    private MailRecipient toRecipient(MailRecipientRequest request) {
        validateRecipient(request);
        MailRecipient recipient = new MailRecipient();
        recipient.setEmail(request.getEmail());
        recipient.setName(request.getName());
        recipient.setIsEnabled(Optional.ofNullable(request.getIsEnabled()).orElse(true));
        return recipient;
    }

    private void validateRecipient(MailRecipientRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
    }
}
