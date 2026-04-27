package top.ziyoung.webhooknotification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import run.halo.app.plugin.SettingFetcher;
import top.ziyoung.webhooknotification.model.NotificationEvent;
import top.ziyoung.webhooknotification.model.NotificationEventType;
import top.ziyoung.webhooknotification.model.NotificationSubject;
import top.ziyoung.webhooknotification.notify.WebhookDispatcher;
import top.ziyoung.webhooknotification.notify.WebhookPayloadBuilder;
import top.ziyoung.webhooknotification.settings.WebhookEndpoint;

class WebhookNotificationPluginTest {

    @Test
    void shouldBuildGenericJsonPayload() {
        var endpoint = new WebhookEndpoint().setFormat("generic-json");
        var event = sampleEvent();

        var request = WebhookPayloadBuilder.build(endpoint, event);

        assertEquals("application/json", request.contentType().toString());
        var payload = assertInstanceOf(Map.class, request.body());
        assertEquals("comment_created", payload.get("event"));
        assertEquals("收到评论：示例文章", payload.get("title"));
    }

    @Test
    void shouldBuildNtfyPayload() {
        var endpoint = new WebhookEndpoint().setFormat("ntfy-markdown");
        var event = sampleEvent();

        var request = WebhookPayloadBuilder.build(endpoint, event);

        assertEquals("text/plain", request.contentType().toString());
        assertTrue(request.headers().containsKey("Title"));
        assertEquals("Comment received", request.headers().get("Title"));
        assertEquals("yes", request.headers().get("Markdown"));
        assertEquals(event.markdownBody(), request.body());
    }

    @Test
    void shouldBuildSlackPayload() {
        var endpoint = new WebhookEndpoint().setFormat("slack-compatible");
        var event = sampleEvent();

        var request = WebhookPayloadBuilder.build(endpoint, event);

        var payload = assertInstanceOf(Map.class, request.body());
        assertEquals(event.markdownBody(), payload.get("text"));
    }

    @Test
    void shouldRejectEmptyUrlWhenTestingWebhook() {
        var dispatcher = new WebhookDispatcher(Mockito.mock(SettingFetcher.class));

        var result = dispatcher.test(new WebhookEndpoint().setUrl(""));

        assertTrue(!result.success());
        assertEquals("ValidationError", result.errorType());
    }

    private NotificationEvent sampleEvent() {
        return new NotificationEvent(
            NotificationEventType.COMMENT_CREATED,
            "收到评论：示例文章",
            "Halo 收到新的评论",
            "收到评论：示例文章\n\nauthor: Alice",
            "## 收到评论：示例文章\n\n- author：Alice",
            Instant.parse("2026-04-20T12:00:00Z"),
            new NotificationSubject("示例文章", "https://example.com/posts/demo"),
            Map.of("author", "Alice")
        );
    }
}
