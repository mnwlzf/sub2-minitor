package com.sub2.monitor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 统一 HTTP/HTTPS 请求客户端。
 *
 * 设计目标：
 * 1. 支持任意对象作为 POST 请求体
 * 2. 支持直接请求、代理请求、附加请求头请求
 * 3. 返回状态码、响应头、响应体
 * 4. 按 sessionKey 保存会话头和业务标识
 * 5. 避免重载歧义，使用语义明确的方法名
 */
@Component
public class HttpsClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, HttpClient> proxyClientCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, String>> sessionHeaderCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> idCache = new ConcurrentHashMap<>();
    private final HttpClient directClient;

    public HttpsClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.directClient = createClient(null);
    }

    /**
     * 代理配置。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProxyConfig {
        /**
         * 是否启用代理；false 时调用方会直接走直连客户端。
         */
        private boolean enabled;

        /**
         * 代理类型，当前由业务层传入 http / socks / socks5。
         */
        private String type;

        /**
         * 代理主机地址。
         */
        private String host;

        /**
         * 代理端口。
         */
        private int port;
    }

    /**
     * 统一响应对象。
     */
    @Data
    @AllArgsConstructor
    public static class HttpResult {
        private int statusCode;
        private Map<String, List<String>> headers;
        private String body;
    }

    /**
     * 直接发送 GET 请求。
     *
     * @param url     完整请求地址
     * @param headers 请求头
     * @return 完整响应对象
     */
    public HttpResult getDirect(String url, Map<String, String> headers) {
        return sendGet(url, headers, null, directClient);
    }

    /**
     * 通过代理发送 GET 请求。
     *
     * @param url         完整请求地址
     * @param headers     请求头
     * @param proxyConfig 代理配置
     * @return 完整响应对象
     */
    public HttpResult getWithProxy(String url, Map<String, String> headers, ProxyConfig proxyConfig) {
        return sendGet(url, headers, null, resolveClient(proxyConfig));
    }

    /**
     * 直接发送 POST 请求。
     *
     * @param url     完整请求地址
     * @param headers 请求头
     * @param body    请求体，支持 String 和任意 POJO
     * @return 完整响应对象
     */
    public HttpResult postDirect(String url, Map<String, String> headers, Object body) {
        return sendPost(url, headers, body, null, directClient);
    }

    public void postDirectStream(String url, Map<String, String> headers, Object body, Consumer<String> chunkConsumer) {
        sendPostStream(url, headers, body, null, directClient, chunkConsumer);
    }

    public void postWithProxyStream(String url, Map<String, String> headers, Object body, ProxyConfig proxyConfig,
                                    Consumer<String> chunkConsumer) {
        sendPostStream(url, headers, body, null, resolveClient(proxyConfig), chunkConsumer);
    }

    /**
     * 通过代理发送 POST 请求。
     *
     * @param url         完整请求地址
     * @param headers     请求头
     * @param body        请求体，支持 String 和任意 POJO
     * @param proxyConfig 代理配置
     * @return 完整响应对象
     */
    public HttpResult postWithProxy(String url, Map<String, String> headers, Object body, ProxyConfig proxyConfig) {
        return sendPost(url, headers, body, null, resolveClient(proxyConfig));
    }

    /**
     * 直接发送 GET 请求，并追加额外请求头。
     *
     * @param url          完整请求地址
     * @param headers      基础请求头
     * @param extraHeaders 额外请求头
     * @return 完整响应对象
     */
    public HttpResult getWithExtraHeaders(String url, Map<String, String> headers, Map<String, String> extraHeaders) {
        return sendGet(url, headers, extraHeaders, directClient);
    }

    /**
     * 直接发送 POST 请求，并追加额外请求头。
     *
     * @param url          完整请求地址
     * @param headers      基础请求头
     * @param body         请求体，支持 String 和任意 POJO
     * @param extraHeaders 额外请求头
     * @return 完整响应对象
     */
    public HttpResult postWithExtraHeaders(String url, Map<String, String> headers, Object body, Map<String, String> extraHeaders) {
        return sendPost(url, headers, body, extraHeaders, directClient);
    }

    /**
     * 仅获取 GET 的响应体。
     */
    public String getBodyDirect(String url, Map<String, String> headers) {
        return getDirect(url, headers).getBody();
    }

    public String getBodyWithProxy(String url, Map<String, String> headers, ProxyConfig proxyConfig) {
        return getWithProxy(url, headers, proxyConfig).getBody();
    }

    /**
     * 仅获取 POST 的响应体。
     */
    public String postBodyDirect(String url, Map<String, String> headers, Object body) {
        return postDirect(url, headers, body).getBody();
    }

    /**
     * 保存某个 sessionKey 对应的会话请求头。
     */
    public void putSessionHeaders(String sessionKey, Map<String, String> headers) {
        if (sessionKey == null || sessionKey.isBlank()) {
            throw new IllegalArgumentException("sessionKey is required");
        }
        if (headers == null || headers.isEmpty()) {
            sessionHeaderCache.remove(sessionKey);
            return;
        }
        sessionHeaderCache.put(sessionKey, new java.util.LinkedHashMap<>(headers));
    }

    /**
     * 获取某个 sessionKey 对应的会话请求头副本。
     */
    public Map<String, String> getSessionHeaders(String sessionKey) {
        Map<String, String> headers = sessionHeaderCache.get(sessionKey);
        if (headers == null) {
            return null;
        }
        return new java.util.LinkedHashMap<>(headers);
    }

    /**
     * 清理某个 sessionKey 的会话请求头。
     */
    public void clearSessionHeaders(String sessionKey) {
        if (sessionKey != null) {
            sessionHeaderCache.remove(sessionKey);
        }
    }

    /**
     * 保存某个 sessionKey 对应的 id。
     */
    public void putId(String sessionKey, String id) {
        if (sessionKey == null || sessionKey.isBlank()) {
            throw new IllegalArgumentException("sessionKey is required");
        }
        if (id == null || id.isBlank()) {
            idCache.remove(sessionKey);
            return;
        }
        idCache.put(sessionKey, id);
    }

    /**
     * 获取某个 sessionKey 对应的 id。
     */
    public String getId(String sessionKey) {
        return idCache.get(sessionKey);
    }

    /**
     * 清理某个 sessionKey 对应的 id。
     */
    public void clearId(String sessionKey) {
        if (sessionKey != null) {
            idCache.remove(sessionKey);
        }
    }

    private HttpResult sendGet(String url, Map<String, String> headers, Map<String, String> extraHeaders, HttpClient client) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET();
        applyHeaders(requestBuilder, headers, extraHeaders);
        return send(requestBuilder.build(), client);
    }

    private HttpResult sendPost(String url, Map<String, String> headers, Object body, Map<String, String> extraHeaders, HttpClient client) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(serializeBody(body)));
        applyHeaders(requestBuilder, headers, extraHeaders);
        ensureContentType(requestBuilder, headers, extraHeaders);
        return send(requestBuilder.build(), client);
    }

    private HttpClient resolveClient(ProxyConfig proxyConfig) {
        if (proxyConfig == null || !proxyConfig.isEnabled()) {
            return directClient;
        }
        // 同一代理配置复用同一个 HttpClient，避免每次请求都创建新客户端。
        String cacheKey = proxyConfig.getType() + ":" + proxyConfig.getHost() + ":" + proxyConfig.getPort();
        return proxyClientCache.computeIfAbsent(cacheKey, key -> createClient(proxyConfig));
    }

    private HttpClient createClient(ProxyConfig proxyConfig) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT);
        if (proxyConfig != null && proxyConfig.isEnabled()) {
            InetSocketAddress address = new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort());
            if ("socks5".equalsIgnoreCase(proxyConfig.getType()) || "socks".equalsIgnoreCase(proxyConfig.getType())) {
                // SOCKS 代理使用自定义 ProxySelector，确保 HttpClient 走 SOCKS 通道。
                clientBuilder.proxy(new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {
                        return List.of(new Proxy(Proxy.Type.SOCKS, address));
                    }

                    @Override
                    public void connectFailed(URI uri, java.net.SocketAddress sa, IOException ioe) {
                    }
                });
            } else {
                clientBuilder.proxy(ProxySelector.of(address));
            }
        }
        return clientBuilder.build();
    }

    private void ensureContentType(HttpRequest.Builder requestBuilder, Map<String, String> headers, Map<String, String> extraHeaders) {
        Map<String, String> allHeaders = mergeHeaders(headers, extraHeaders);
        if (allHeaders == null || allHeaders.keySet().stream().map(String::toLowerCase).noneMatch("content-type"::equals)) {
            requestBuilder.header("Content-Type", "application/json; charset=UTF-8");
        }
    }

    private void applyHeaders(HttpRequest.Builder requestBuilder, Map<String, String> headers, Map<String, String> extraHeaders) {
        Map<String, String> allHeaders = mergeHeaders(headers, extraHeaders);
        if (allHeaders == null || allHeaders.isEmpty()) {
            return;
        }
        allHeaders.forEach(requestBuilder::header);
    }

    private Map<String, String> mergeHeaders(Map<String, String> headers, Map<String, String> extraHeaders) {
        if ((headers == null || headers.isEmpty()) && (extraHeaders == null || extraHeaders.isEmpty())) {
            return null;
        }
        java.util.LinkedHashMap<String, String> mergedHeaders = new java.util.LinkedHashMap<>();
        if (headers != null) {
            mergedHeaders.putAll(headers);
        }
        if (extraHeaders != null) {
            mergedHeaders.putAll(extraHeaders);
        }
        return mergedHeaders;
    }

    private String serializeBody(Object body) {
        if (body == null) {
            return "";
        }
        if (body instanceof String stringBody) {
            return stringBody;
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize request body", exception);
        }
    }

    private HttpResult send(HttpRequest request, HttpClient client) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.headers().map(), response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("HTTP request I/O error", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted", exception);
        }
    }

    private void sendPostStream(String url, Map<String, String> headers, Object body, Map<String, String> extraHeaders,
                                HttpClient client, Consumer<String> chunkConsumer) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(serializeBody(body)));
        applyHeaders(requestBuilder, headers, extraHeaders);
        ensureContentType(requestBuilder, headers, extraHeaders);
        requestBuilder.header("Accept", "text/event-stream");
        requestBuilder.header("Cache-Control", "no-cache");
        try {
            HttpResponse<InputStream> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream inputStream = response.body()) {
                StringBuilder buffer = new StringBuilder();
                byte[] bytes = new byte[1024];
                int read;
                while ((read = inputStream.read(bytes)) != -1) {
                    String chunk = new String(bytes, 0, read);
                    buffer.append(chunk);
                    chunkConsumer.accept(chunk);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("HTTP stream request I/O error", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP stream request interrupted", exception);
        }
    }
}
