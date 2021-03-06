package dev.dohpaz.phpExtras.php.config.library;

import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.jetbrains.php.config.library.PhpIncludePathManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class SortIncludePathListener implements ModuleRootListener {
    private final PhpIncludePathManager includePathManager;

    public SortIncludePathListener(PhpIncludePathManager includePathManager) {
        this.includePathManager = includePathManager;
    }

    public void rootsChanged(@NotNull ModuleRootEvent event) {
        final List<String> includePaths = this.includePathManager.getIncludePath();

        includePaths.sort((o1, o2) -> {
            if (Objects.equals(o1, o2))
                return 0;
            if (o1 == null)
                return 1;
            if (o2 == null)
                return -1;
            return o1.compareTo(o2);
        });

        this.includePathManager.setIncludePath(includePaths);
    }
}
