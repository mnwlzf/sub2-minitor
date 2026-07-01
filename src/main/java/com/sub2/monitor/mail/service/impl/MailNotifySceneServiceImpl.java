package com.sub2.monitor.mail.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.mail.dto.MailNotifySceneRequest;
import com.sub2.monitor.mail.dto.MailNotifySceneResponse;
import com.sub2.monitor.mail.entity.MailNotifyScene;
import com.sub2.monitor.mail.entity.MailNotifySceneRecipient;
import com.sub2.monitor.mail.entity.MailRecipient;
import com.sub2.monitor.mail.entity.MailSmtpConfig;
import com.sub2.monitor.mail.mapper.MailNotifySceneMapper;
import com.sub2.monitor.mail.mapper.MailNotifySceneRecipientMapper;
import com.sub2.monitor.mail.mapper.MailRecipientMapper;
import com.sub2.monitor.mail.mapper.MailSmtpConfigMapper;
import com.sub2.monitor.mail.service.MailNotifySceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MailNotifySceneServiceImpl extends ServiceImpl<MailNotifySceneMapper, MailNotifyScene> implements MailNotifySceneService {

    private final MailNotifySceneRecipientMapper sceneRecipientMapper;
    private final MailRecipientMapper mailRecipientMapper;
    private final MailSmtpConfigMapper mailSmtpConfigMapper;

    @Override
    public List<MailNotifySceneResponse> listScenes() {
        List<MailNotifyScene> scenes = list(new LambdaQueryWrapper<MailNotifyScene>().orderByDesc(MailNotifyScene::getId));
        Map<Long, MailSmtpConfig> smtpMap = mailSmtpConfigMapper.selectList(null).stream()
                .collect(Collectors.toMap(MailSmtpConfig::getId, Function.identity(), (left, right) -> left));
        Map<Long, MailRecipient> recipientMap = mailRecipientMapper.selectList(null).stream()
                .collect(Collectors.toMap(MailRecipient::getId, Function.identity(), (left, right) -> left));
        return scenes.stream()
                .map(scene -> toResponse(scene, smtpMap.get(scene.getSmtpConfigId()), recipientMap))
                .toList();
    }

    @Override
    @Transactional
    public MailNotifySceneResponse createScene(MailNotifySceneRequest request) {
        MailNotifyScene scene = new MailNotifyScene();
        applyRequest(scene, request);
        save(scene);
        syncRecipients(scene.getId(), request);
        return toResponse(getById(scene.getId()));
    }

    @Override
    @Transactional
    public MailNotifySceneResponse updateScene(Long id, MailNotifySceneRequest request) {
        MailNotifyScene scene = getSceneOrThrow(id);
        applyRequest(scene, request);
        updateById(scene);
        syncRecipients(id, request);
        return toResponse(getById(id));
    }

    @Override
    @Transactional
    public void deleteScene(Long id) {
        getSceneOrThrow(id);
        sceneRecipientMapper.delete(new LambdaQueryWrapper<MailNotifySceneRecipient>()
                .eq(MailNotifySceneRecipient::getSceneId, id));
        removeById(id);
    }

    private void applyRequest(MailNotifyScene scene, MailNotifySceneRequest request) {
        if (!StringUtils.hasText(request.getSceneCode())) {
            throw new IllegalArgumentException("sceneCode 不能为空");
        }
        if (!StringUtils.hasText(request.getSceneName())) {
            throw new IllegalArgumentException("sceneName 不能为空");
        }
        scene.setSceneCode(request.getSceneCode().trim().toUpperCase());
        scene.setSceneName(request.getSceneName().trim());
        scene.setDescription(trimToNull(request.getDescription()));
        scene.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        scene.setSmtpConfigId(request.getSmtpConfigId());
        scene.setSubjectTemplate(trimToNull(request.getSubjectTemplate()));
        scene.setContentTemplate(trimToNull(request.getContentTemplate()));
        if (scene.getCreatedAt() == null) {
            scene.setCreatedAt(LocalDateTime.now());
        }
        scene.setUpdatedAt(LocalDateTime.now());
    }

    private void syncRecipients(Long sceneId, MailNotifySceneRequest request) {
        sceneRecipientMapper.delete(new LambdaQueryWrapper<MailNotifySceneRecipient>()
                .eq(MailNotifySceneRecipient::getSceneId, sceneId));
        insertRecipients(sceneId, request.getToRecipientIds(), "TO");
        insertRecipients(sceneId, request.getCcRecipientIds(), "CC");
        insertRecipients(sceneId, request.getBccRecipientIds(), "BCC");
    }

    private void insertRecipients(Long sceneId, List<Long> recipientIds, String type) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        Set<Long> distinctIds = recipientIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        distinctIds.forEach(recipientId -> {
            MailNotifySceneRecipient relation = new MailNotifySceneRecipient();
            relation.setSceneId(sceneId);
            relation.setRecipientId(recipientId);
            relation.setRecipientType(type);
            relation.setCreatedAt(LocalDateTime.now());
            sceneRecipientMapper.insert(relation);
        });
    }

    private MailNotifySceneResponse toResponse(MailNotifyScene scene) {
        Map<Long, MailSmtpConfig> smtpMap = mailSmtpConfigMapper.selectList(null).stream()
                .collect(Collectors.toMap(MailSmtpConfig::getId, Function.identity(), (left, right) -> left));
        Map<Long, MailRecipient> recipientMap = mailRecipientMapper.selectList(null).stream()
                .collect(Collectors.toMap(MailRecipient::getId, Function.identity(), (left, right) -> left));
        return toResponse(scene, smtpMap.get(scene.getSmtpConfigId()), recipientMap);
    }

    private MailNotifySceneResponse toResponse(
            MailNotifyScene scene,
            MailSmtpConfig smtpConfig,
            Map<Long, MailRecipient> recipientMap
    ) {
        MailNotifySceneResponse response = new MailNotifySceneResponse();
        response.setId(scene.getId());
        response.setSceneCode(scene.getSceneCode());
        response.setSceneName(scene.getSceneName());
        response.setDescription(scene.getDescription());
        response.setEnabled(scene.getEnabled());
        response.setSmtpConfigId(scene.getSmtpConfigId());
        response.setSmtpConfigName(smtpConfig == null ? null : smtpConfig.getConfigName());
        response.setSubjectTemplate(scene.getSubjectTemplate());
        response.setContentTemplate(scene.getContentTemplate());
        response.setCreatedAt(scene.getCreatedAt());
        response.setUpdatedAt(scene.getUpdatedAt());

        List<MailNotifySceneRecipient> relations = sceneRecipientMapper.selectList(new LambdaQueryWrapper<MailNotifySceneRecipient>()
                .eq(MailNotifySceneRecipient::getSceneId, scene.getId())
                .orderByAsc(MailNotifySceneRecipient::getId));
        response.setToRecipients(toRecipientItems(relations, recipientMap, "TO"));
        response.setCcRecipients(toRecipientItems(relations, recipientMap, "CC"));
        response.setBccRecipients(toRecipientItems(relations, recipientMap, "BCC"));
        return response;
    }

    private List<MailNotifySceneResponse.RecipientItem> toRecipientItems(
            List<MailNotifySceneRecipient> relations,
            Map<Long, MailRecipient> recipientMap,
            String type
    ) {
        return relations.stream()
                .filter(item -> type.equalsIgnoreCase(item.getRecipientType()))
                .map(item -> {
                    MailRecipient recipient = recipientMap.get(item.getRecipientId());
                    if (recipient == null) {
                        return null;
                    }
                    MailNotifySceneResponse.RecipientItem response = new MailNotifySceneResponse.RecipientItem();
                    response.setId(recipient.getId());
                    response.setEmail(recipient.getEmail());
                    response.setRecipientName(recipient.getRecipientName());
                    return response;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private MailNotifyScene getSceneOrThrow(Long id) {
        MailNotifyScene scene = getById(id);
        if (scene == null) {
            throw new IllegalArgumentException("通知场景不存在: " + id);
        }
        return scene;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
