package dev.dohpaz.phpExtras;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class NotificationUtil {
    static public void error(@NotNull Project project, @NotNull String module, @NotNull String message) {
        NotificationUtil.send(project, module, message, NotificationType.ERROR);
    }

    static public void info(@NotNull Project project, @NotNull String module, @NotNull String message) {
        NotificationUtil.send(project, module, message, NotificationType.INFORMATION);
    }

    static public void send(@NotNull Project project, @NotNull String module, @NotNull String message, @NotNull NotificationType notificationType) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("PHP Extras")
                .createNotification("Module include path changed", "[" + module + "] " + message, notificationType)
                .notify(project);
    }

    static public void warn(@NotNull Project project, @NotNull String module, @NotNull String message) {
        NotificationUtil.send(project, module, message, NotificationType.WARNING);
    }
}
