package top.ziyoung.webhooknotification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.ziyoung.webhooknotification.watcher.HaloResourceWatcherRegistrar;

@Slf4j
@Component
public class WebhookNotificationPlugin extends BasePlugin {

    private final HaloResourceWatcherRegistrar watcherRegistrar;

    public WebhookNotificationPlugin(PluginContext pluginContext,
        HaloResourceWatcherRegistrar watcherRegistrar) {
        super(pluginContext);
        this.watcherRegistrar = watcherRegistrar;
    }

    @Override
    public void start() {
        watcherRegistrar.start();
        log.info("Webhook notification plugin started.");
    }

    @Override
    public void stop() {
        watcherRegistrar.stop();
        log.info("Webhook notification plugin stopped.");
    }
}
