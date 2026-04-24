package top.ziyoung.webhooknotification.settings;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class WebhookSettings {
    private List<WebhookEndpoint> endpoints = new ArrayList<>();
}
