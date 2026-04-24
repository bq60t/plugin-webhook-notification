package top.ziyoung.webhooknotification.api;

import lombok.Builder;

@Builder
public record WebhookTestResponse(
    boolean success,
    Integer statusCode,
    String message,
    String responseBody,
    String errorType
) {
}
