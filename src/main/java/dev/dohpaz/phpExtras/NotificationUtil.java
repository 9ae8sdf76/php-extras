package dev.dohpaz.phpExtras;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
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
        NotificationGroup group = NotificationGroup.findRegisteredGroup("PHP Extras");

        if (group == null) {
            return;
        }

        Notification notification = group.createNotification("Module include path removed", module + ": " + message, notificationType);
        ApplicationManager.getApplication().invokeLater(() -> {
            Notifications.Bus.notify(notification, project);
        });
    }

    static public void warn(@NotNull Project project, @NotNull String module, @NotNull String message) {
        NotificationUtil.send(project, module, message, NotificationType.WARNING);
    }
}
