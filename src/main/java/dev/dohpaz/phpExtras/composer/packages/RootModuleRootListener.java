package dev.dohpaz.phpExtras.composer.packages;

import com.google.gson.JsonObject;
import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.php.composer.ComposerConfigUtils;
import com.jetbrains.php.config.library.PhpIncludePathManager;
import dev.dohpaz.phpExtras.NotificationUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class RootModuleRootListener implements ModuleRootListener {
    final private VirtualFile composerJson;
    final private PhpIncludePathManager includePathManager;
    final private LocalFileSystem localFileSystem;

    public RootModuleRootListener(PhpIncludePathManager includePathManager, LocalFileSystem localFileSystem, VirtualFile composerJson) {
        this.composerJson = composerJson;
        this.includePathManager = includePathManager;
        this.localFileSystem = localFileSystem;
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

        ApplicationManager.getApplication().invokeLater(() -> {
            final Project project = (Project) event.getSource();
            final String basePath = project.getBasePath();
            String module = "unknown";

            try {
                List<String> toRemove = new LinkedList<>();

                Pair<String, String> composerDirectories = ComposerConfigUtils.getVendorAndBinDirs(composerJson);
                String vendorDirectory = basePath + "/" + (composerDirectories != null ? composerDirectories.getFirst() : "vendor");

                VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
                List<String> includePaths = includePathManager.getIncludePath();

                for (VirtualFile contentRoot : contentRoots) {
                    String contentRootPath = contentRoot.getCanonicalPath();
                    VirtualFile contentComposerJson = localFileSystem.findFileByPath(contentRootPath + "/composer.json");

                    if (contentComposerJson == null) {
                        continue;
                    }

                    JsonObject jsonObject = ComposerConfigUtils.parseJson(contentComposerJson).getAsJsonObject();
                    module = jsonObject.get("name").getAsString();
                    VirtualFile compositeFile = localFileSystem.findFileByPath(vendorDirectory + "/" + module);

                    if (compositeFile == null || !localFileSystem.exists(compositeFile)) {
                        continue;
                    }

                    String canonicalCompositePath = compositeFile.getPath();
                    for (String includePath : includePaths) {
                        if (includePath.equals(canonicalCompositePath)) {
                            NotificationUtil.info(project, module, includePath);
                            toRemove.add(includePath);
                        }
                    }
                }

                includePaths.removeAll(toRemove);
                includePathManager.setIncludePath(includePaths);
            } catch (IOException e) {
                NotificationUtil.error(project, module, e.toString());
                e.printStackTrace();
            }

            project.getMessageBus().syncPublisher(ProjectTopics.PROJECT_ROOTS);
        }, ModalityState.defaultModalityState());
    }
}
