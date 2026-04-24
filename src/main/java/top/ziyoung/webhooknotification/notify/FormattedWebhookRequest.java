package top.ziyoung.webhooknotification.notify;

import java.util.Map;
import org.springframework.http.MediaType;

public record FormattedWebhookRequest(
    MediaType contentType,
    Object body,
    Map<String, String> headers
) {
}
