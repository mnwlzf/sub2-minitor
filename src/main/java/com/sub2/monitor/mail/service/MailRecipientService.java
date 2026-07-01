package com.sub2.monitor.mail.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sub2.monitor.mail.dto.MailRecipientRequest;
import com.sub2.monitor.mail.dto.MailRecipientResponse;
import com.sub2.monitor.mail.entity.MailRecipient;

import java.util.List;

public interface MailRecipientService extends IService<MailRecipient> {

    List<MailRecipientResponse> listRecipients();

    MailRecipientResponse createRecipient(MailRecipientRequest request);

    MailRecipientResponse updateRecipient(Long id, MailRecipientRequest request);

    void deleteRecipient(Long id);
}
