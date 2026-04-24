package top.ziyoung.webhooknotification.watcher;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.Device;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.content.Reply;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.Watcher;
import top.ziyoung.webhooknotification.notify.WebhookEventPublisher;

@Slf4j
@Component
@RequiredArgsConstructor
public class HaloResourceWatcherRegistrar {

    private final ExtensionClient extensionClient;
    private final WebhookEventPublisher publisher;
    private final AtomicReference<Watcher> watcherRef = new AtomicReference<>();
    private volatile Instant startedAt = Instant.now();

    public void start() {
        if (watcherRef.get() != null) {
            return;
        }
        startedAt = Instant.now();
        var watcher = new Watcher() {
            private volatile boolean disposed;
            private Runnable disposeHook;

            @Override
            public void onAdd(Extension extension) {
                if (disposed) {
                    return;
                }
                // Halo may replay existing resources when a watcher is first registered, so only
                // treat recently-created resources as new notification events.
                if (extension instanceof Comment comment && isFresh(comment)) {
                    publisher.publishCommentCreated(comment);
                } else if (extension instanceof Reply reply && isFresh(reply)) {
                    publisher.publishReplyCreated(reply);
                } else if (extension instanceof Device device && isFresh(device)) {
                    publisher.publishNewDeviceLogin(device);
                }
            }

            @Override
            public void onUpdate(Extension oldExtension, Extension newExtension) {
                if (disposed) {
                    return;
                }
                // Password change is inferred from User updates because Halo does not expose it as
                // a dedicated shared event in the plugin API.
                if (oldExtension instanceof User oldUser && newExtension instanceof User newUser) {
                    publisher.publishPasswordChanged(oldUser, newUser);
                }
            }

            @Override
            public void registerDisposeHook(Runnable dispose) {
                this.disposeHook = dispose;
            }

            @Override
            public void dispose() {
                this.disposed = true;
                if (disposeHook != null) {
                    disposeHook.run();
                }
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }
        };
        if (watcherRef.compareAndSet(null, watcher)) {
            extensionClient.watch(watcher);
            log.info("Halo resource watcher registered.");
        }
    }

    public void stop() {
        var watcher = watcherRef.getAndSet(null);
        if (watcher != null) {
            watcher.dispose();
            log.info("Halo resource watcher disposed.");
        }
    }

    private boolean isFresh(Extension extension) {
        // Allow a small clock skew window so resources created around plugin startup are not missed.
        var timestamp = extension.getMetadata().getCreationTimestamp();
        return timestamp != null && !timestamp.isBefore(startedAt.minusSeconds(5));
    }
}
