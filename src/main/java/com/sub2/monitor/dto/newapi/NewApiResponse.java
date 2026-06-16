package com.sub2.monitor.dto.newapi;

import lombok.Data;

import java.util.List;

/**
 * newapi 响应结果对象。
 *
 * 用于承载 newapi 接口返回的原始字段或解析后的业务字段。
 */
@Data
public class NewApiResponse {

    /**
     * 原始响应内容。
     */
    private String rawBody;

    /**
     * 按账号执行时的结果列表。
     */
    private List<AccountResult> accountResults;

    /**
     * 按账号执行时的渠道结果列表。
     */
    private List<ChannelResult> channelResults;

    @Data
    public static class AccountResult {

        /**
         * 用户名。
         */
        private String userName;

        /**
         * 结果类型，例如 balance / channels / model。
         */
        private String type;

        /**
         * 原始响应。
         */
        private String rawBody;
    }

    @Data
    public static class ChannelResult {

        /**
         * 用户名。
         */
        private String userName;

        /**
         * 分组名。
         */
        private String groupName;

        /**
         * 描述。
         */
        private String desc;

        /**
         * 倍率。
         */
        private Double ratio;
    }
}
