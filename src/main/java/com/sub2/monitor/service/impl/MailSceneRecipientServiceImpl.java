package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.MailSceneRecipient;
import com.sub2.monitor.mapper.MailSceneRecipientMapper;
import com.sub2.monitor.service.MailSceneRecipientService;
import org.springframework.stereotype.Service;

@Service
public class MailSceneRecipientServiceImpl extends ServiceImpl<MailSceneRecipientMapper, MailSceneRecipient> implements MailSceneRecipientService {
}
