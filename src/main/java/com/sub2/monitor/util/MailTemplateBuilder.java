package com.sub2.monitor.util;

public final class MailTemplateBuilder {

    private MailTemplateBuilder() {
    }

    public static String page(String title, String subtitle, String body) {
        return "<div style=\"margin:0;padding:0;background:#f5f7fb;color:#111827;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'PingFang SC','Microsoft YaHei',sans-serif;\">"
                + "<div style=\"max-width:760px;margin:0 auto;padding:18px 12px 28px;\">"
                + "<div style=\"background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">"
                + "<div style=\"padding:20px 18px;background:#0f172a;color:#ffffff;\">"
                + "<div style=\"font-size:20px;font-weight:700;line-height:1.35;\">" + escapeHtml(title) + "</div>"
                + "<div style=\"margin-top:8px;color:#cbd5e1;font-size:13px;line-height:1.6;\">" + escapeHtml(subtitle) + "</div>"
                + "</div>"
                + "<div style=\"padding:16px 14px;\">"
                + body
                + "</div>"
                + "</div>"
                + "<div style=\"padding:12px 4px 0;color:#94a3b8;font-size:12px;line-height:1.5;text-align:center;\">Sub2 Monitor</div>"
                + "</div>"
                + "</div>";
    }

    public static String summaryGrid(String... labelValuePairs) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div style=\"margin:0 0 14px;\">");
        for (int i = 0; i + 1 < labelValuePairs.length; i += 2) {
            builder.append("<div style=\"display:inline-block;vertical-align:top;width:31%;min-width:150px;margin:0 6px 8px 0;padding:12px;border:1px solid #e5e7eb;border-radius:10px;background:#f8fafc;box-sizing:border-box;\">")
                    .append("<div style=\"color:#64748b;font-size:12px;line-height:1.4;\">").append(escapeHtml(labelValuePairs[i])).append("</div>")
                    .append("<div style=\"margin-top:6px;color:#0f172a;font-size:18px;font-weight:700;line-height:1.25;word-break:break-word;\">")
                    .append(escapeHtml(labelValuePairs[i + 1]))
                    .append("</div>")
                    .append("</div>");
        }
        builder.append("</div>");
        return builder.toString();
    }

    public static String section(String title, String meta, String body) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div style=\"margin:14px 0 0;padding:14px;border:1px solid #e5e7eb;border-radius:12px;background:#ffffff;\">")
                .append("<div style=\"color:#0f172a;font-size:17px;font-weight:700;line-height:1.35;word-break:break-word;\">")
                .append(escapeHtml(title))
                .append("</div>");
        if (meta != null && !meta.isBlank()) {
            builder.append("<div style=\"margin-top:6px;color:#64748b;font-size:12px;line-height:1.6;word-break:break-word;\">")
                    .append(escapeHtml(meta))
                    .append("</div>");
        }
        builder.append("<div style=\"margin-top:12px;\">").append(body).append("</div></div>");
        return builder.toString();
    }

    public static String changeRow(String type, String name, String oldValue, String newValue, String color) {
        return "<div style=\"margin:8px 0;padding:10px 12px;border:1px solid #e5e7eb;border-left:4px solid " + color + ";border-radius:10px;background:#fbfdff;\">"
                + "<div style=\"font-size:12px;font-weight:700;color:" + color + ";line-height:1.4;\">" + escapeHtml(type) + "</div>"
                + "<div style=\"margin-top:4px;color:#111827;font-size:15px;font-weight:700;line-height:1.45;word-break:break-word;\">" + escapeHtml(name) + "</div>"
                + "<div style=\"margin-top:8px;color:#475569;font-size:13px;line-height:1.6;\">"
                + "<span style=\"display:inline-block;min-width:72px;color:#64748b;\">原倍率</span><strong style=\"color:#111827;\">" + escapeHtml(oldValue) + "</strong>"
                + "<br><span style=\"display:inline-block;min-width:72px;color:#64748b;\">新倍率</span><strong style=\"color:#111827;\">" + escapeHtml(newValue) + "</strong>"
                + "</div>"
                + "</div>";
    }

    public static String accountRow(String username,
                                    String startBalance,
                                    String endBalance,
                                    String platformConsume,
                                    String actualConsume,
                                    String firstTime,
                                    String lastTime) {
        return "<div style=\"margin:8px 0;padding:12px;border:1px solid #e5e7eb;border-radius:10px;background:#fbfdff;\">"
                + "<div style=\"color:#111827;font-size:15px;font-weight:700;line-height:1.45;word-break:break-word;\">" + escapeHtml(username) + "</div>"
                + "<div style=\"margin-top:8px;\">"
                + metric("开始余额", startBalance)
                + metric("结束余额", endBalance)
                + metric("平台消耗", platformConsume)
                + metric("实际消耗", actualConsume)
                + "</div>"
                + "<div style=\"margin-top:8px;color:#64748b;font-size:12px;line-height:1.55;word-break:break-word;\">"
                + "首条：" + escapeHtml(firstTime) + "<br>"
                + "末条：" + escapeHtml(lastTime)
                + "</div>"
                + "</div>";
    }

    private static String metric(String label, String value) {
        return "<div style=\"display:inline-block;width:48%;min-width:120px;margin:0 4px 8px 0;padding:8px;border-radius:8px;background:#f1f5f9;box-sizing:border-box;\">"
                + "<div style=\"color:#64748b;font-size:12px;line-height:1.35;\">" + escapeHtml(label) + "</div>"
                + "<div style=\"margin-top:4px;color:#0f172a;font-size:15px;font-weight:700;line-height:1.25;word-break:break-word;\">" + escapeHtml(value) + "</div>"
                + "</div>";
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
