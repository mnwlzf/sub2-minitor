package com.sub2.monitor.collect.sub2api.service.impl;

import cn.hutool.json.JSONUtil;
import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2ApiLoginRequest;
import com.sub2.monitor.collect.sub2api.dto.Sub2ApiLoginRes;
import com.sub2.monitor.collect.sub2api.service.Sub2ApiCollectService;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class Sub2ApiCollectServiceImpl implements Sub2ApiCollectService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private RedisTemplate redisTemplate; // 预留扩展

    public LoginResponse<Sub2ApiLoginRes> login(String baseUrl) {
        LoginResponse<Sub2ApiLoginRes> response = null;
        List<Account> accounts = accountMapper.selectSub2apiAccounts(baseUrl);
        if (accounts.isEmpty()) {
            log.info("没有需要登录的账号 (baseUrl={})", baseUrl);
            return response;
        }

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                .defaultHeader(HttpHeaders.ORIGIN, baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                                .responseTimeout(Duration.ofSeconds(10))
                ))
                .build();

        for (Account account : accounts) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待间隔期间线程被中断，停止后续登录");
                return response;
            }

            Sub2ApiLoginRequest request = new Sub2ApiLoginRequest();
            request.setEmail(account.getEmail());
            request.setPassword(account.getPassword());

            try {
                response = client.post()
                        .uri("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchangeToMono(clientResponse -> {
                            HttpStatusCode status = clientResponse.statusCode();
                            if (status.is4xxClientError()) {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("账号 {} 登录失败，状态码 {}，响应体：{}",
                                                    account.getEmail(), status, errorBody);
                                            return Mono.error(new WebClientResponseException(
                                                    "Client error: " + errorBody,
                                                    status.value(),
                                                    status.toString(),
                                                    null,
                                                    errorBody.getBytes(StandardCharsets.UTF_8),
                                                    StandardCharsets.UTF_8
                                            ));
                                        });
                            } else if (status.is5xxServerError()) {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.warn("账号 {} 服务端错误，状态码 {}，响应体：{}",
                                                    account.getEmail(), status, errorBody);
                                            return Mono.error(new WebClientResponseException(
                                                    "Server error: " + errorBody,
                                                    status.value(),
                                                    status.toString(),
                                                    null,
                                                    errorBody.getBytes(StandardCharsets.UTF_8),
                                                    StandardCharsets.UTF_8
                                            ));
                                        });
                            } else {
                                // 成功响应：提取响应体并封装
                                return clientResponse.bodyToMono(Sub2ApiLoginRes.class)
                                        .map(body -> {
                                            LoginResponse<Sub2ApiLoginRes> wrapper = new LoginResponse<>();
                                            wrapper.setHeaders(clientResponse.headers().asHttpHeaders());
                                            wrapper.setBody(body);
                                            return wrapper;
                                        });
                            }
                        })
                        .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5)))
                        .block();

                log.info("账号 {} 登录成功，响应：{}", account.getEmail(), response);
                return response;
            } catch (WebClientResponseException e) {
                String errorBody = e.getResponseBodyAsString();
                log.error("账号 {} 登录失败（最终），HTTP状态码: {}, 响应体: {}",
                        account.getEmail(), e.getStatusCode(), errorBody);
            } catch (Exception e) {
                Throwable rootCause = Exceptions.unwrap(e);
                log.error("账号 {} 登录失败，异常类型: {}, 原因: {}",
                        account.getEmail(), rootCause.getClass().getSimpleName(), rootCause.getMessage(), rootCause);
            }
        }
        return response;
    }


}