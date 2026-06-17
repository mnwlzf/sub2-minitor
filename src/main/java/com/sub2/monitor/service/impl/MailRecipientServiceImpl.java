package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.MailRecipient;
import com.sub2.monitor.mapper.MailRecipientMapper;
import com.sub2.monitor.service.MailRecipientService;
import org.springframework.stereotype.Service;

@Service
public class MailRecipientServiceImpl extends ServiceImpl<MailRecipientMapper, MailRecipient> implements MailRecipientService {
}
