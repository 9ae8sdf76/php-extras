package dev.dohpaz.phpExtras.composer.packages;

import com.google.gson.JsonObject;
import com.google.gson.stream.MalformedJsonException;
import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.composer.ComposerConfigUtils;
import com.jetbrains.php.config.library.PhpIncludePathManager;
import dev.dohpaz.phpExtras.NotificationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class RootModuleRootListener implements ModuleRootListener {
    final private VirtualFile composerJson;
    final private PhpIncludePathManager includePathManager;
    final private LocalFileSystem localFileSystem;

    private VirtualFile[] contentRoots;

    public RootModuleRootListener(PhpIncludePathManager includePathManager, LocalFileSystem localFileSystem, VirtualFile composerJson) {
        this.composerJson = composerJson;
        this.includePathManager = includePathManager;
        this.localFileSystem = localFileSystem;
    }

    @Override
    public void beforeRootsChange(@NotNull ModuleRootEvent event) {
        contentRoots = getContentRoots(event.getProject());
    }

    public void rootsChanged(@NotNull ModuleRootEvent event) {
        /*
         * Search the project's composer.json for any module content roots for the project's autoload-dev. If the
         * definition(s) exist on the file system, then remove them from the vendor folder of the project's include
         * paths.
         */
        if (this.composerJson == null) {
            return;
        }

        final Project project = event.getProject();

        final VirtualFile[] contentRoots = getContentRoots(project);

        // If the global contentRoots has more roots than the local contentRoots, then we are removing a content root.
        if (contentRoots.length > this.contentRoots.length) {
            removeIncludePath(project);
        } else {
            addIncludePath(project);
        }
    }

    /**
     * Add back to the project include paths the path that was removed as a content root.
     *
     * @param project The IntelliJ project
     */
    private void addIncludePath(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            List<String> includePaths = getIncludePaths();
            String vendorDirectory = getVendorDirectory(project);

            String module = "unknown";

            Collection<VirtualFile> contentRoots;

            // If someone were to go to Preferences > Directories and remove all content roots
            // from the project, then the global contentRoots will be null.
            if (this.contentRoots != null) {
                contentRoots = ContainerUtil.subtract(Arrays.asList(this.contentRoots), Arrays.asList(getContentRoots(project)));
            } else {
                contentRoots = Arrays.asList(getContentRoots(project));
            }

            for (VirtualFile contentRoot : contentRoots) {
                VirtualFile compositeFile;

                try {
                    module = getPackageName(contentRoot);

                    if (module == null) {
                        continue;
                    }

                    compositeFile = localFileSystem.findFileByPath(vendorDirectory + "/" + module);
                } catch (IOException e) {
                    NotificationUtil.warn(project, module != null ? module : "unknown", "Exception: " + e);
                    continue;
                }

                if (compositeFile == null || !localFileSystem.exists(compositeFile)) {
                    continue;
                }

                String path = compositeFile.getPath();

                if (!includePaths.contains(path)) {
                    NotificationUtil.info(project, module, "[A] " + path);
                    includePaths.add(path);
                }
            }

            includePathManager.setIncludePath(includePaths);

            project.getMessageBus().syncPublisher(ProjectTopics.PROJECT_ROOTS);
            this.contentRoots = null;
        });
    }

    /**
     * Remove the path from the global include paths that was added as a content root.
     *
     * @param project The IntelliJ project
     */
    private void removeIncludePath(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            List<String> includePaths = getIncludePaths();
            final VirtualFile[] contentRoots = getContentRoots(project);

            String module = "unknown";

            try {
                List<String> toRemove = new LinkedList<>();
                String vendorDirectory = getVendorDirectory(project);

                for (VirtualFile contentRoot : contentRoots) {
                    VirtualFile compositeFile;

                    try {
                        module = getPackageName(contentRoot);

                        if (module == null || module.equals("")) {
                            continue;
                        }

                        compositeFile = localFileSystem.findFileByPath(vendorDirectory + "/" + module);
                    } catch (MalformedJsonException e) {
                        NotificationUtil.warn(project, module != null ? module : "unknown", "Exception: " + e);
                        continue;
                    }

                    if (compositeFile == null || !localFileSystem.exists(compositeFile)) {
                        continue;
                    }

                    String path = compositeFile.getPath();

                    if (includePaths.contains(path)) {
                        for (String includePath : includePaths) {
                            if (includePath.equals(path)) {
                                NotificationUtil.info(project, module, "[R] " + includePath);
                                toRemove.add(includePath);
                            }
                        }
                    }
                }

                if (toRemove.size() > 0) {
                    includePaths.removeAll(toRemove);
                    includePathManager.setIncludePath(includePaths);
                }
            } catch (IOException e) {
                NotificationUtil.error(project, module != null ? module : "unknown", e.toString());
                e.printStackTrace();
            }

            project.getMessageBus().syncPublisher(ProjectTopics.PROJECT_ROOTS);
            this.contentRoots = null;
        });
    }

    @NotNull
    private VirtualFile[] getContentRoots(Project project) {
        return ProjectRootManager.getInstance(project).getContentRoots();
    }

    private List<String> getIncludePaths() {
        return includePathManager.getIncludePath();
    }

    private @Nullable String getPackageName(VirtualFile root) throws IOException {
        String contentRootPath = root.getCanonicalPath();
        VirtualFile contentComposerJson = localFileSystem.findFileByPath(contentRootPath + "/composer.json");

        if (contentComposerJson == null) {
            return null;
        }

        JsonObject jsonObject = ComposerConfigUtils.parseJson(contentComposerJson).getAsJsonObject();

        return jsonObject.has("name")
            ? jsonObject.get("name").getAsString()
            : null;
    }

    private String getVendorDirectory(Project project) {
        final String basePath = project.getBasePath();

        Pair<String, String> composerDirectories = ComposerConfigUtils.getVendorAndBinDirs(composerJson);
        return basePath + "/" + (composerDirectories != null ? composerDirectories.getFirst() : "vendor");
    }
}
