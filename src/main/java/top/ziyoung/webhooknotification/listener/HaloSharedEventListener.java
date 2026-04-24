package top.ziyoung.webhooknotification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.halo.app.event.user.UserLoginEvent;
import top.ziyoung.webhooknotification.notify.WebhookEventPublisher;

@Component
@RequiredArgsConstructor
public class HaloSharedEventListener {

    private final WebhookEventPublisher publisher;

    @EventListener
    public void onUserLogin(UserLoginEvent event) {
        publisher.publishUserLogin(event.getUser());
    }
}
