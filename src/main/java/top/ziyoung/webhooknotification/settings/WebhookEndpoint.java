package top.ziyoung.webhooknotification.settings;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class WebhookEndpoint {
    private boolean enabled = true;
    private String name = "";
    private String url = "";
    private String format = "generic-json";
    private String headersJson = "{}";
}
