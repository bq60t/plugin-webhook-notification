package top.ziyoung.webhooknotification.model;

public enum NotificationEventType {
    USER_LOGIN("user_login", "用户登录", "unlock"),
    NEW_DEVICE_LOGIN("new_device_login", "新设备登录", "computer"),
    PASSWORD_CHANGED("password_changed", "修改密码", "lock"),
    COMMENT_CREATED("comment_created", "收到评论", "speech_balloon"),
    REPLY_CREATED("reply_created", "评论被回复", "left_speech_bubble");

    private final String code;
    private final String label;
    private final String ntfyTag;

    NotificationEventType(String code, String label, String ntfyTag) {
        this.code = code;
        this.label = label;
        this.ntfyTag = ntfyTag;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String ntfyTag() {
        return ntfyTag;
    }
}
