package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.dto.MailSceneRecipientRequest;
import com.sub2.monitor.dto.MailSceneRequest;
import com.sub2.monitor.dto.MailSceneResponse;
import com.sub2.monitor.entity.MailNotificationScene;
import com.sub2.monitor.entity.MailRecipient;
import com.sub2.monitor.entity.MailSceneRecipient;
import com.sub2.monitor.mapper.MailNotificationSceneMapper;
import com.sub2.monitor.mapper.MailRecipientMapper;
import com.sub2.monitor.mapper.MailSceneRecipientMapper;
import com.sub2.monitor.service.MailNotificationSceneService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MailNotificationSceneServiceImpl extends ServiceImpl<MailNotificationSceneMapper, MailNotificationScene> implements MailNotificationSceneService {

    private final MailSceneRecipientMapper mailSceneRecipientMapper;
    private final MailRecipientMapper mailRecipientMapper;

    public MailNotificationSceneServiceImpl(MailSceneRecipientMapper mailSceneRecipientMapper,
                                            MailRecipientMapper mailRecipientMapper) {
        this.mailSceneRecipientMapper = mailSceneRecipientMapper;
        this.mailRecipientMapper = mailRecipientMapper;
    }

    @Override
    public List<MailSceneResponse> listScenes() {
        List<MailNotificationScene> scenes = list(new LambdaQueryWrapper<MailNotificationScene>()
                .orderByAsc(MailNotificationScene::getId));
        List<MailSceneRecipient> relations = mailSceneRecipientMapper.selectList(new LambdaQueryWrapper<MailSceneRecipient>()
                .orderByAsc(MailSceneRecipient::getId));
        List<Long> recipientIds = relations.stream()
                .map(MailSceneRecipient::getRecipientId)
                .distinct()
                .toList();
        Map<Long, MailRecipient> recipientMap = recipientIds.isEmpty()
                ? Map.of()
                : mailRecipientMapper.selectBatchIds(recipientIds)
                .stream()
                .collect(Collectors.toMap(MailRecipient::getId, recipient -> recipient));
        Map<String, List<MailSceneRecipient>> relationMap = relations.stream()
                .collect(Collectors.groupingBy(MailSceneRecipient::getSceneKey));

        return scenes.stream()
                .map(scene -> toSceneResponse(scene, relationMap.getOrDefault(scene.getSceneKey(), List.of()), recipientMap))
                .toList();
    }

    @Override
    public MailNotificationScene saveScene(MailSceneRequest request) {
        validateScene(request, true);
        MailNotificationScene scene = new MailNotificationScene();
        scene.setSceneKey(request.getSceneKey());
        scene.setSceneName(request.getSceneName());
        scene.setDescription(request.getDescription());
        scene.setIsEnabled(request.getIsEnabled() == null || request.getIsEnabled());
        scene.setCreateTime(OffsetDateTime.now());
        scene.setUpdateTime(OffsetDateTime.now());
        save(scene);
        return scene;
    }

    @Override
    public MailNotificationScene updateScene(MailSceneRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        validateScene(request, false);
        MailNotificationScene scene = getById(request.getId());
        if (scene == null) {
            throw new IllegalArgumentException("通知场景不存在");
        }
        scene.setSceneName(request.getSceneName());
        scene.setDescription(request.getDescription());
        scene.setIsEnabled(request.getIsEnabled() == null || request.getIsEnabled());
        scene.setUpdateTime(OffsetDateTime.now());
        updateById(scene);
        return scene;
    }

    @Override
    public void deleteScene(Long id) {
        MailNotificationScene scene = getById(id);
        if (scene == null) {
            return;
        }
        mailSceneRecipientMapper.delete(new LambdaQueryWrapper<MailSceneRecipient>()
                .eq(MailSceneRecipient::getSceneKey, scene.getSceneKey()));
        removeById(id);
    }

    @Override
    public void addRecipient(MailSceneRecipientRequest request) {
        if (request.getSceneKey() == null || request.getSceneKey().isBlank()) {
            throw new IllegalArgumentException("通知场景不能为空");
        }
        if (request.getRecipientId() == null) {
            throw new IllegalArgumentException("收件人不能为空");
        }
        String recipientType = request.getRecipientType() == null || request.getRecipientType().isBlank()
                ? "TO"
                : request.getRecipientType().toUpperCase();
        if (!List.of("TO", "CC", "BCC").contains(recipientType)) {
            throw new IllegalArgumentException("收件人类型必须是 TO、CC 或 BCC");
        }
        MailSceneRecipient exists = mailSceneRecipientMapper.selectOne(new LambdaQueryWrapper<MailSceneRecipient>()
                .eq(MailSceneRecipient::getSceneKey, request.getSceneKey())
                .eq(MailSceneRecipient::getRecipientId, request.getRecipientId())
                .eq(MailSceneRecipient::getRecipientType, recipientType));
        if (exists != null) {
            return;
        }
        MailSceneRecipient relation = new MailSceneRecipient();
        relation.setSceneKey(request.getSceneKey());
        relation.setRecipientId(request.getRecipientId());
        relation.setRecipientType(recipientType);
        relation.setCreateTime(OffsetDateTime.now());
        mailSceneRecipientMapper.insert(relation);
    }

    @Override
    public void removeRecipient(Long relationId) {
        if (relationId != null) {
            mailSceneRecipientMapper.deleteById(relationId);
        }
    }

    private MailSceneResponse toSceneResponse(MailNotificationScene scene,
                                              List<MailSceneRecipient> relations,
                                              Map<Long, MailRecipient> recipientMap) {
        MailSceneResponse response = new MailSceneResponse();
        response.setId(scene.getId());
        response.setSceneKey(scene.getSceneKey());
        response.setSceneName(scene.getSceneName());
        response.setDescription(scene.getDescription());
        response.setIsEnabled(scene.getIsEnabled());
        response.setRecipients(relations.stream()
                .map(relation -> toSceneRecipient(relation, recipientMap.get(relation.getRecipientId())))
                .filter(recipient -> recipient.getRecipientId() != null)
                .toList());
        return response;
    }

    private MailSceneResponse.SceneRecipient toSceneRecipient(MailSceneRecipient relation, MailRecipient recipient) {
        MailSceneResponse.SceneRecipient response = new MailSceneResponse.SceneRecipient();
        response.setRelationId(relation.getId());
        response.setRecipientId(relation.getRecipientId());
        response.setRecipientType(relation.getRecipientType());
        if (recipient != null) {
            response.setEmail(recipient.getEmail());
            response.setName(recipient.getName());
            response.setIsEnabled(recipient.getIsEnabled());
        }
        return response;
    }

    private void validateScene(MailSceneRequest request, boolean validateKeyUnique) {
        if (request.getSceneName() == null || request.getSceneName().isBlank()) {
            throw new IllegalArgumentException("场景名称不能为空");
        }
        if (validateKeyUnique) {
            if (request.getSceneKey() == null || request.getSceneKey().isBlank()) {
                throw new IllegalArgumentException("场景标识不能为空");
            }
            if (!request.getSceneKey().matches("[a-z][a-z0-9_]{1,99}")) {
                throw new IllegalArgumentException("场景标识只能使用小写字母、数字和下划线，并以小写字母开头");
            }
            MailNotificationScene exists = getOne(new LambdaQueryWrapper<MailNotificationScene>()
                    .eq(MailNotificationScene::getSceneKey, request.getSceneKey()));
            if (exists != null) {
                throw new IllegalArgumentException("场景标识已存在");
            }
        }
    }
}
