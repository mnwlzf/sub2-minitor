package com.sub2.monitor.mail.controller;

import com.sub2.monitor.mail.dto.MailNotifySceneRequest;
import com.sub2.monitor.mail.dto.MailNotifySceneResponse;
import com.sub2.monitor.mail.dto.MailRecipientRequest;
import com.sub2.monitor.mail.dto.MailRecipientResponse;
import com.sub2.monitor.mail.dto.MailSmtpConfigRequest;
import com.sub2.monitor.mail.dto.MailSmtpConfigResponse;
import com.sub2.monitor.mail.service.MailNotifySceneService;
import com.sub2.monitor.mail.service.MailRecipientService;
import com.sub2.monitor.mail.service.MailSmtpConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailSmtpConfigService mailSmtpConfigService;
    private final MailRecipientService mailRecipientService;
    private final MailNotifySceneService mailNotifySceneService;

    @GetMapping("/smtp-configs")
    public List<MailSmtpConfigResponse> listSmtpConfigs() {
        return mailSmtpConfigService.listConfigs();
    }

    @PostMapping("/smtp-configs")
    public MailSmtpConfigResponse createSmtpConfig(@RequestBody MailSmtpConfigRequest request) {
        return mailSmtpConfigService.createConfig(request);
    }

    @PutMapping("/smtp-configs/{id}")
    public MailSmtpConfigResponse updateSmtpConfig(@PathVariable Long id, @RequestBody MailSmtpConfigRequest request) {
        return mailSmtpConfigService.updateConfig(id, request);
    }

    @DeleteMapping("/smtp-configs/{id}")
    public void deleteSmtpConfig(@PathVariable Long id) {
        mailSmtpConfigService.deleteConfig(id);
    }

    @PostMapping("/smtp-configs/test")
    public void testSmtpConfig(@RequestBody MailSmtpConfigRequest request) {
        mailSmtpConfigService.testConnection(request);
    }

    @GetMapping("/recipients")
    public List<MailRecipientResponse> listRecipients() {
        return mailRecipientService.listRecipients();
    }

    @PostMapping("/recipients")
    public MailRecipientResponse createRecipient(@RequestBody MailRecipientRequest request) {
        return mailRecipientService.createRecipient(request);
    }

    @PutMapping("/recipients/{id}")
    public MailRecipientResponse updateRecipient(@PathVariable Long id, @RequestBody MailRecipientRequest request) {
        return mailRecipientService.updateRecipient(id, request);
    }

    @DeleteMapping("/recipients/{id}")
    public void deleteRecipient(@PathVariable Long id) {
        mailRecipientService.deleteRecipient(id);
    }

    @GetMapping("/scenes")
    public List<MailNotifySceneResponse> listScenes() {
        return mailNotifySceneService.listScenes();
    }

    @PostMapping("/scenes")
    public MailNotifySceneResponse createScene(@RequestBody MailNotifySceneRequest request) {
        return mailNotifySceneService.createScene(request);
    }

    @PutMapping("/scenes/{id}")
    public MailNotifySceneResponse updateScene(@PathVariable Long id, @RequestBody MailNotifySceneRequest request) {
        return mailNotifySceneService.updateScene(id, request);
    }

    @DeleteMapping("/scenes/{id}")
    public void deleteScene(@PathVariable Long id) {
        mailNotifySceneService.deleteScene(id);
    }
}
