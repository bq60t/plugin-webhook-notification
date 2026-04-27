package top.ziyoung.webhooknotification.notify;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import top.ziyoung.webhooknotification.model.NotificationEvent;
import top.ziyoung.webhooknotification.settings.WebhookEndpoint;

public final class WebhookPayloadBuilder {

    private WebhookPayloadBuilder() {
    }

    public static FormattedWebhookRequest build(WebhookEndpoint endpoint, NotificationEvent event) {
        return switch (normalizeFormat(endpoint.getFormat())) {
            case "ntfy-markdown" -> buildNtfy(event);
            case "slack-compatible" -> buildSlack(event);
            default -> buildGenericJson(event);
        };
    }

    private static String normalizeFormat(String format) {
        return Objects.requireNonNullElse(format, "generic-json").trim().toLowerCase();
    }

    private static FormattedWebhookRequest buildGenericJson(NotificationEvent event) {
        var body = new LinkedHashMap<String, Object>();
        body.put("event", event.type().code());
        body.put("eventLabel", event.type().label());
        body.put("title", event.title());
        body.put("summary", event.summary());
        body.put("text", event.plainBody());
        body.put("markdown", event.markdownBody());
        body.put("timestamp", event.timestamp());
        body.put("attributes", event.attributes());
        if (event.subject() != null) {
            body.put("subject", Map.of(
                "title", event.subject().title(),
                "url", Objects.requireNonNullElse(event.subject().url(), "")
            ));
        }
        return new FormattedWebhookRequest(MediaType.APPLICATION_JSON, body, Map.of());
    }

    private static FormattedWebhookRequest buildNtfy(NotificationEvent event) {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Title", ntfyTitle(event));
        headers.put("Markdown", "yes");
        headers.put("Tags", event.type().ntfyTag());
        if (event.subject() != null && event.subject().url() != null && !event.subject().url().isBlank()) {
            headers.put("Click", event.subject().url());
        }
        return new FormattedWebhookRequest(MediaType.TEXT_PLAIN, event.markdownBody(), headers);
    }

    private static String ntfyTitle(NotificationEvent event) {
        return switch (event.type()) {
            case COMMENT_CREATED -> "Comment received";
            case REPLY_CREATED -> "Reply received";
            case PASSWORD_CHANGED -> "Password changed";
            case USER_LOGIN -> "User login";
            case NEW_DEVICE_LOGIN -> "New device login";
        };
    }

    private static FormattedWebhookRequest buildSlack(NotificationEvent event) {
        return new FormattedWebhookRequest(
            MediaType.APPLICATION_JSON,
            Map.of("text", event.markdownBody()),
            Map.of()
        );
    }
}
