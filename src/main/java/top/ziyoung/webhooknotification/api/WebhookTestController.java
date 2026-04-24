package top.ziyoung.webhooknotification.api;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.halo.app.plugin.ApiVersion;
import run.halo.app.plugin.SettingFetcher;
import top.ziyoung.webhooknotification.notify.WebhookDispatcher;
import top.ziyoung.webhooknotification.settings.WebhookEndpoint;
import top.ziyoung.webhooknotification.settings.WebhookSettings;

@Validated
@RestController
@RequiredArgsConstructor
@ApiVersion("api.console.halo.run/v1alpha1")
@RequestMapping("/plugins/webhook-notification")
public class WebhookTestController {

    private final WebhookDispatcher webhookDispatcher;
    private final SettingFetcher settingFetcher;

    @PostMapping(value = "/test-webhook", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookTestResponse> testWebhook(
        @Valid @RequestBody WebhookTestRequest request) {
        var endpoint = new WebhookEndpoint()
            .setEnabled(true)
            .setName("manual-test")
            .setUrl(request.getUrl())
            .setFormat(request.getFormat())
            .setHeadersJson(request.getHeadersJson());

        var result = webhookDispatcher.test(endpoint);
        return ResponseEntity.status(result.success() ? 200 : 400).body(result);
    }

    @GetMapping(value = "/webhook-endpoints", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SavedWebhookEndpointView> listWebhookEndpoints() {
        // The UI uses the saved settings snapshot to render per-endpoint "test current config"
        // actions without duplicating Halo's native settings form.
        var settings = settingFetcher.fetch("webhooks", WebhookSettings.class)
            .orElseGet(WebhookSettings::new);
        var endpoints = settings.getEndpoints();
        return java.util.stream.IntStream.range(0, endpoints.size())
            .mapToObj(index -> {
                var endpoint = endpoints.get(index);
                return SavedWebhookEndpointView.builder()
                    .index(index)
                    .enabled(endpoint.isEnabled())
                    .name(endpoint.getName())
                    .url(endpoint.getUrl())
                    .format(endpoint.getFormat())
                    .headersJson(endpoint.getHeadersJson())
                    .build();
            })
            .toList();
    }

    @PostMapping(value = "/test-webhook/{index}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookTestResponse> testSavedWebhook(@PathVariable("index") int index) {
        // Resolve by index from the latest saved settings so the test always targets the same
        // configuration currently visible to production notification delivery.
        var settings = settingFetcher.fetch("webhooks", WebhookSettings.class)
            .orElseGet(WebhookSettings::new);
        var endpoints = settings.getEndpoints();
        if (index < 0 || index >= endpoints.size()) {
            return ResponseEntity.badRequest().body(WebhookTestResponse.builder()
                .success(false)
                .message("Webhook 目标不存在或索引已过期，请刷新后重试。")
                .errorType("NotFound")
                .build());
        }
        var endpoint = endpoints.get(index);
        var result = webhookDispatcher.test(endpoint);
        return ResponseEntity.status(result.success() ? 200 : 400).body(result);
    }
}
