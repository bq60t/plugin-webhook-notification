package top.ziyoung.webhooknotification.notify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.plugin.SettingFetcher;
import top.ziyoung.webhooknotification.api.WebhookTestResponse;
import top.ziyoung.webhooknotification.model.NotificationEvent;
import top.ziyoung.webhooknotification.model.NotificationEventType;
import top.ziyoung.webhooknotification.model.NotificationSubject;
import top.ziyoung.webhooknotification.settings.EventSettings;
import top.ziyoung.webhooknotification.settings.WebhookEndpoint;
import top.ziyoung.webhooknotification.settings.WebhookSettings;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final SettingFetcher settingFetcher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder().build();

    public void dispatch(NotificationEvent event) {
        // Webhook delivery should never block Halo event handling threads.
        Mono.fromRunnable(() -> doDispatch(event))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(error -> log.error("Failed to dispatch webhook event {}", event.type().code(), error))
            .subscribe();
    }

    private void doDispatch(NotificationEvent event) {
        // Read the latest saved plugin settings on each dispatch so changes take effect immediately.
        var eventSettings = settingFetcher.fetch("events", EventSettings.class)
            .orElseGet(EventSettings::new);
        if (!eventSettings.isEnabled(event.type())) {
            return;
        }

        var webhookSettings = settingFetcher.fetch("webhooks", WebhookSettings.class)
            .orElseGet(WebhookSettings::new);

        webhookSettings.getEndpoints().stream()
            .filter(WebhookEndpoint::isEnabled)
            .filter(endpoint -> endpoint.getUrl() != null && !endpoint.getUrl().isBlank())
            .forEach(endpoint -> send(endpoint, event));
    }

    private void send(WebhookEndpoint endpoint, NotificationEvent event) {
        var request = WebhookPayloadBuilder.build(endpoint, event);
        var headers = new HttpHeaders();
        headers.setContentType(request.contentType());
        mergeHeaders(headers, request.headers());
        mergeHeaders(headers, parseHeaders(endpoint));

        webClient.post()
            .uri(endpoint.getUrl())
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .bodyValue(request.body())
            .retrieve()
            .toBodilessEntity()
            .block(REQUEST_TIMEOUT);

        log.debug("Webhook sent to [{}] for event [{}].", endpoint.getName(), event.type().code());
    }

    public WebhookTestResponse test(WebhookEndpoint endpoint) {
        // The test path reuses the same formatter and header parser as production delivery so the
        // result reflects the real saved webhook configuration as closely as possible.
        if (endpoint.getUrl() == null || endpoint.getUrl().isBlank()) {
            return WebhookTestResponse.builder()
                .success(false)
                .message("Webhook 地址不能为空。")
                .errorType("ValidationError")
                .build();
        }
        try {
            var request = WebhookPayloadBuilder.build(endpoint, buildTestEvent());
            var headers = new HttpHeaders();
            headers.setContentType(request.contentType());
            mergeHeaders(headers, request.headers());
            mergeHeaders(headers, parseHeaders(endpoint));

            var response = webClient.post()
                .uri(endpoint.getUrl())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(request.body())
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> WebhookTestResponse.builder()
                        .success(clientResponse.statusCode().is2xxSuccessful())
                        .statusCode(clientResponse.statusCode().value())
                        .message(clientResponse.statusCode().is2xxSuccessful()
                            ? "Webhook 测试成功。"
                            : "Webhook 返回了非 2xx 状态码。")
                        .responseBody(body)
                        .errorType(clientResponse.statusCode().is2xxSuccessful() ? "" : "HttpError")
                        .build()))
                .block(REQUEST_TIMEOUT);

            return response == null ? WebhookTestResponse.builder()
                .success(false)
                .message("Webhook 未返回响应。")
                .errorType("EmptyResponse")
                .build() : response;
        } catch (WebClientResponseException ex) {
            return WebhookTestResponse.builder()
                .success(false)
                .statusCode(ex.getStatusCode().value())
                .message(ex.getMessage())
                .responseBody(ex.getResponseBodyAsString())
                .errorType(ex.getClass().getSimpleName())
                .build();
        } catch (WebClientRequestException ex) {
            return WebhookTestResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorType(ex.getClass().getSimpleName())
                .build();
        } catch (Exception ex) {
            return WebhookTestResponse.builder()
                .success(false)
                .message(ex.getMessage() == null ? ex.toString() : ex.getMessage())
                .errorType(ex.getClass().getSimpleName())
                .build();
        }
    }

    private Map<String, String> parseHeaders(WebhookEndpoint endpoint) {
        if (endpoint.getHeadersJson() == null || endpoint.getHeadersJson().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            // Halo stores custom headers as JSON text in settings, so convert them lazily here.
            var raw = objectMapper.readValue(endpoint.getHeadersJson(),
                new TypeReference<Map<String, Object>>() {
                });
            var headers = new LinkedHashMap<String, String>();
            raw.forEach((key, value) -> headers.put(key, value == null ? "" : String.valueOf(value)));
            return headers;
        } catch (Exception ex) {
            log.warn("Ignore invalid headersJson for endpoint [{}].", endpoint.getName(), ex);
            return Collections.emptyMap();
        }
    }

    private void mergeHeaders(HttpHeaders target, Map<String, String> source) {
        source.forEach(target::set);
        if (target.getContentType() == null) {
            target.setContentType(MediaType.APPLICATION_JSON);
        }
    }

    private NotificationEvent buildTestEvent() {
        // Keep the test payload deterministic enough for debugging, but representative of a real
        // notification so markdown/slack/ntfy formatters can be verified with one request.
        var subject = new NotificationSubject("Webhook 连通性测试", "https://docs.halo.run");
        return new NotificationEvent(
            NotificationEventType.COMMENT_CREATED,
            "Webhook 测试通知",
            "这是一条由 Halo Webhook Notification 插件发送的测试消息。",
            "Webhook 测试通知\n\n这是一条由 Halo Webhook Notification 插件发送的测试消息。",
            "## Webhook 测试通知\n\n这是一条由 Halo Webhook Notification 插件发送的测试消息。\n\n- source：Halo Plugin\n- purpose：Connectivity Test",
            java.time.Instant.now(),
            subject,
            Map.of(
                "source", "Halo Plugin",
                "purpose", "Connectivity Test"
            )
        );
    }
}
