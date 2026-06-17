package com.sub2.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sub2.monitor.entity.MailSendLog;
import com.sub2.monitor.mapper.MailSendLogMapper;
import com.sub2.monitor.service.MailSendLogService;
import org.springframework.stereotype.Service;

@Service
public class MailSendLogServiceImpl extends ServiceImpl<MailSendLogMapper, MailSendLog> implements MailSendLogService {
}
