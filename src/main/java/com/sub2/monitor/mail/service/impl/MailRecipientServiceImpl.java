package com.sub2.monitor.mail.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.mail.dto.MailRecipientRequest;
import com.sub2.monitor.mail.dto.MailRecipientResponse;
import com.sub2.monitor.mail.entity.MailRecipient;
import com.sub2.monitor.mail.mapper.MailRecipientMapper;
import com.sub2.monitor.mail.service.MailRecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MailRecipientServiceImpl extends ServiceImpl<MailRecipientMapper, MailRecipient> implements MailRecipientService {

    @Override
    public List<MailRecipientResponse> listRecipients() {
        return list().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public MailRecipientResponse createRecipient(MailRecipientRequest request) {
        MailRecipient recipient = new MailRecipient();
        applyRequest(recipient, request);
        save(recipient);
        return toResponse(getById(recipient.getId()));
    }

    @Override
    @Transactional
    public MailRecipientResponse updateRecipient(Long id, MailRecipientRequest request) {
        MailRecipient recipient = getRecipientOrThrow(id);
        applyRequest(recipient, request);
        updateById(recipient);
        return toResponse(getById(id));
    }

    @Override
    @Transactional
    public void deleteRecipient(Long id) {
        getRecipientOrThrow(id);
        removeById(id);
    }

    private void applyRequest(MailRecipient recipient, MailRecipientRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("email 不能为空");
        }
        recipient.setEmail(request.getEmail().trim());
        recipient.setRecipientName(trimToNull(request.getRecipientName()));
        recipient.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        recipient.setRemark(trimToNull(request.getRemark()));
    }

    private MailRecipient getRecipientOrThrow(Long id) {
        MailRecipient recipient = getById(id);
        if (recipient == null) {
            throw new IllegalArgumentException("收件人不存在: " + id);
        }
        return recipient;
    }

    private MailRecipientResponse toResponse(MailRecipient recipient) {
        MailRecipientResponse response = new MailRecipientResponse();
        response.setId(recipient.getId());
        response.setEmail(recipient.getEmail());
        response.setRecipientName(recipient.getRecipientName());
        response.setEnabled(recipient.getEnabled());
        response.setRemark(recipient.getRemark());
        response.setCreatedAt(recipient.getCreatedAt());
        response.setUpdatedAt(recipient.getUpdatedAt());
        return response;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
