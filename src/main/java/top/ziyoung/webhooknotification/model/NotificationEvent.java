package top.ziyoung.webhooknotification.model;

import java.time.Instant;
import java.util.Map;

public record NotificationEvent(
    NotificationEventType type,
    String title,
    String summary,
    String plainBody,
    String markdownBody,
    Instant timestamp,
    NotificationSubject subject,
    Map<String, String> attributes
) {
}
