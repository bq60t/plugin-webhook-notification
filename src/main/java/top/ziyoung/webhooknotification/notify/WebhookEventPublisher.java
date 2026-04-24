package top.ziyoung.webhooknotification.notify;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.Device;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.content.Reply;
import run.halo.app.extension.ExtensionClient;
import top.ziyoung.webhooknotification.model.NotificationEvent;
import top.ziyoung.webhooknotification.model.NotificationEventType;
import top.ziyoung.webhooknotification.model.NotificationSubject;

@Component
@RequiredArgsConstructor
public class WebhookEventPublisher {

    private final WebhookDispatcher dispatcher;
    private final CommentSubjectResolver subjectResolver;
    private final ExtensionClient extensionClient;

    public void publishUserLogin(User user) {
        var title = "用户登录：" + displayUser(user);
        var details = orderedMap(
            "user", user.getMetadata().getName(),
            "displayName", emptyToDash(user.getSpec().getDisplayName()),
            "email", emptyToDash(user.getSpec().getEmail())
        );
        dispatcher.dispatch(new NotificationEvent(
            NotificationEventType.USER_LOGIN,
            title,
            "Halo 用户登录成功",
            joinLines(title, formatDetails(details)),
            "## " + title + "\n\n" + formatMarkdownDetails(details),
            Instant.now(),
            profileSubject(user),
            details
        ));
    }

    public void publishNewDeviceLogin(Device device) {
        var title = "新设备登录：" + device.getSpec().getPrincipalName();
        var details = orderedMap(
            "user", emptyToDash(device.getSpec().getPrincipalName()),
            "ipAddress", emptyToDash(device.getSpec().getIpAddress()),
            "browser", emptyToDash(device.getStatus().getBrowser()),
            "os", emptyToDash(device.getStatus().getOs()),
            "userAgent", emptyToDash(device.getSpec().getUserAgent()),
            "authenticatedAt", String.valueOf(firstNonNull(
                device.getSpec().getLastAuthenticatedTime(),
                device.getMetadata().getCreationTimestamp(),
                Instant.now()))
        );
        dispatcher.dispatch(new NotificationEvent(
            NotificationEventType.NEW_DEVICE_LOGIN,
            title,
            "检测到新的设备登录记录",
            joinLines(title, formatDetails(details)),
            "## " + title + "\n\n" + formatMarkdownDetails(details),
            Instant.now(),
            null,
            details
        ));
    }

    public void publishPasswordChanged(User oldUser, User newUser) {
        if (Objects.equals(oldUser.getSpec().getPassword(), newUser.getSpec().getPassword())) {
            return;
        }
        if (newUser.getSpec().getPassword() == null || newUser.getSpec().getPassword().isBlank()) {
            return;
        }
        var title = "用户修改密码：" + displayUser(newUser);
        var details = orderedMap(
            "user", newUser.getMetadata().getName(),
            "displayName", emptyToDash(newUser.getSpec().getDisplayName()),
            "email", emptyToDash(newUser.getSpec().getEmail())
        );
        dispatcher.dispatch(new NotificationEvent(
            NotificationEventType.PASSWORD_CHANGED,
            title,
            "检测到用户密码已修改",
            joinLines(title, formatDetails(details)),
            "## " + title + "\n\n" + formatMarkdownDetails(details),
            Instant.now(),
            profileSubject(newUser),
            details
        ));
    }

    public void publishCommentCreated(Comment comment) {
        // Resolve the subject through Halo's comment subject extension point so notifications can
        // link back to the actual post/page instead of only exposing the raw ref name.
        var subject = subjectResolver.resolve(comment.getSpec().getSubjectRef());
        var title = "收到评论：" + subject.title();
        var details = orderedMap(
            "author", commentOwner(comment.getSpec().getOwner()),
            "approved", comment.getSpec().getApproved() ? "已审核" : "待审核",
            "content", excerpt(comment.getSpec().getRaw())
        );
        dispatcher.dispatch(new NotificationEvent(
            NotificationEventType.COMMENT_CREATED,
            title,
            "Halo 收到新的评论",
            joinLines(title, "主题：" + subject.title(), formatDetails(details)),
            "## " + title + "\n\n"
                + "- 主题：" + subject.title() + "\n"
                + formatMarkdownDetails(details),
            Instant.now(),
            subject,
            details
        ));
    }

    public void publishReplyCreated(Reply reply) {
        // Replies only carry the owning comment name, so fetch the comment first to recover the
        // original subject and keep reply notifications consistent with comment notifications.
        var comment = extensionClient.fetch(Comment.class, reply.getSpec().getCommentName()).orElse(null);
        NotificationSubject subject = comment == null
            ? new NotificationSubject("评论：" + reply.getSpec().getCommentName(), "")
            : subjectResolver.resolve(comment.getSpec().getSubjectRef());
        var title = "评论被回复：" + subject.title();
        var details = orderedMap(
            "author", commentOwner(reply.getSpec().getOwner()),
            "approved", reply.getSpec().getApproved() ? "已审核" : "待审核",
            "content", excerpt(reply.getSpec().getRaw()),
            "commentName", reply.getSpec().getCommentName()
        );
        dispatcher.dispatch(new NotificationEvent(
            NotificationEventType.REPLY_CREATED,
            title,
            "Halo 评论出现新的回复",
            joinLines(title, "主题：" + subject.title(), formatDetails(details)),
            "## " + title + "\n\n"
                + "- 主题：" + subject.title() + "\n"
                + formatMarkdownDetails(details),
            Instant.now(),
            subject,
            details
        ));
    }

    private NotificationSubject profileSubject(User user) {
        return new NotificationSubject(displayUser(user), emptyToBlank(user.getStatus().getPermalink()));
    }

    private static String displayUser(User user) {
        var displayName = emptyToBlank(user.getSpec().getDisplayName());
        if (displayName.isBlank()) {
            return user.getMetadata().getName();
        }
        return displayName + "(@" + user.getMetadata().getName() + ")";
    }

    private static String commentOwner(Comment.CommentOwner owner) {
        if (owner == null) {
            return "-";
        }
        if (owner.getDisplayName() != null && !owner.getDisplayName().isBlank()) {
            return owner.getDisplayName();
        }
        return emptyToDash(owner.getName());
    }

    private static String formatDetails(Map<String, String> details) {
        return details.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .reduce((left, right) -> left + "\n" + right)
            .orElse("");
    }

    private static String formatMarkdownDetails(Map<String, String> details) {
        return details.entrySet().stream()
            .map(entry -> "- " + entry.getKey() + "：" + entry.getValue())
            .reduce((left, right) -> left + "\n" + right)
            .orElse("");
    }

    private static String excerpt(String raw) {
        // Webhook targets are usually chat tools, so cap long comment bodies before formatting.
        var value = emptyToBlank(raw).replace("\r", " ").replace("\n", " ").trim();
        if (value.length() <= 180) {
            return value;
        }
        return value.substring(0, 177) + "...";
    }

    private static String joinLines(String... parts) {
        return String.join("\n\n", java.util.Arrays.stream(parts)
            .filter(part -> part != null && !part.isBlank())
            .toList());
    }

    private static Instant firstNonNull(Instant... values) {
        for (Instant value : values) {
            if (value != null) {
                return value;
            }
        }
        return Instant.now();
    }

    private static Map<String, String> orderedMap(String... pairs) {
        var map = new LinkedHashMap<String, String>();
        for (int index = 0; index < pairs.length; index += 2) {
            map.put(pairs[index], pairs[index + 1]);
        }
        return map;
    }

    private static String emptyToDash(String value) {
        return emptyToBlank(value).isBlank() ? "-" : value;
    }

    private static String emptyToBlank(String value) {
        return value == null ? "" : value;
    }
}
