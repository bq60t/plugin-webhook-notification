package top.ziyoung.webhooknotification.notify;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.content.comment.CommentSubject;
import run.halo.app.extension.Ref;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;
import top.ziyoung.webhooknotification.model.NotificationSubject;

@Component
@RequiredArgsConstructor
public class CommentSubjectResolver {

    private final ExtensionGetter extensionGetter;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public NotificationSubject resolve(Ref ref) {
        List<CommentSubject> subjects = extensionGetter.getExtensionList(CommentSubject.class);
        for (CommentSubject subject : subjects) {
            if (!subject.supports(ref)) {
                continue;
            }
            var display = (CommentSubject.SubjectDisplay) subject.getSubjectDisplay(ref.getName())
                .block(Duration.ofSeconds(3));
            if (display != null) {
                return new NotificationSubject(display.title(), display.url());
            }
        }
        return new NotificationSubject(ref.getKind() + ": " + ref.getName(), "");
    }
}
