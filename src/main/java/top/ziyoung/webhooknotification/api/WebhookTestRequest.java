package top.ziyoung.webhooknotification.api;

import lombok.Data;

@Data
public class WebhookTestRequest {
    private String url;
    private String format;
    private String headersJson;
}
