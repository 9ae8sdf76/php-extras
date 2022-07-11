package dev.dohpaz.phpExtras.php;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class PhpExtrasBundle extends DynamicBundle {
    public static final @NonNls String BUNDLE = "messages.PhpExtras";
    private static final PhpExtrasBundle INSTANCE = new PhpExtrasBundle();

    protected PhpExtrasBundle() {
        super("messages.PhpExtras");
    }

    public static @NotNull
    @Nls String message(@NotNull @PropertyKey(
            resourceBundle = "messages.PhpExtras"
    ) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
