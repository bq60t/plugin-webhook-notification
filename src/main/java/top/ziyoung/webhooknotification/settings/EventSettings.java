package top.ziyoung.webhooknotification.settings;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.ziyoung.webhooknotification.model.NotificationEventType;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class EventSettings {
    private boolean userLogin = true;
    private boolean newDeviceLogin = true;
    private boolean passwordChanged = true;
    private boolean commentCreated = true;
    private boolean replyCreated = true;

    public boolean isEnabled(NotificationEventType eventType) {
        return switch (eventType) {
            case USER_LOGIN -> userLogin;
            case NEW_DEVICE_LOGIN -> newDeviceLogin;
            case PASSWORD_CHANGED -> passwordChanged;
            case COMMENT_CREATED -> commentCreated;
            case REPLY_CREATED -> replyCreated;
        };
    }
}
