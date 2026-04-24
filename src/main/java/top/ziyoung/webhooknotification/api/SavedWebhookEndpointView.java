package top.ziyoung.webhooknotification.api;

import lombok.Builder;

@Builder
public record SavedWebhookEndpointView(
    int index,
    boolean enabled,
    String name,
    String url,
    String format,
    String headersJson
) {
}
