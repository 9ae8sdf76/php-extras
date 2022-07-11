package dev.dohpaz.phpExtras.php.codeInspection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.json.psi.JsonElementVisitor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.composer.ComposerConfigUtils;
import com.jetbrains.php.composer.ComposerDataService;
import com.jetbrains.php.composer.InstalledPackageData;
import com.jetbrains.php.lang.inspections.PhpInspection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PhpComposerAutoloadDevPackageToContentRootInspection extends PhpInspection {
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        VirtualFile basePath = localFileSystem.findFileByPath(Objects.requireNonNull(project.getBasePath()));
        ComposerDataService dataService = ComposerDataService.getInstance(holder.getProject());

        if (dataService != null && dataService.isConfigWellConfigured()) {

            VirtualFile config = localFileSystem.refreshAndFindFileByPath(dataService.getConfigPath());

            if (config == null) {
                return PsiElementVisitor.EMPTY_VISITOR;
            }

            // Collect the local packages
            Map<String, String> localRoots = new HashMap<>();
            VirtualFile parentDir = Objects.requireNonNull(basePath).getParent();

            for (String path : localFileSystem.list(parentDir)) {
                VirtualFile moduleDir = localFileSystem.refreshAndFindFileByPath(parentDir.getCanonicalPath() + "/" + path);
                if (moduleDir == null) {
                    continue;
                }

                if (localFileSystem.isDirectory(moduleDir) && !moduleDir.equals(basePath)) {
                    VirtualFile composerFile = localFileSystem.refreshAndFindFileByPath(moduleDir.getCanonicalPath() + "/composer.json");

                    if (composerFile == null || !ComposerDataService.isWellConfigured(composerFile.getCanonicalPath())) {
                        continue;
                    }

                    JsonObject jsonObject;
                    try {
                        jsonObject = ComposerConfigUtils.parseJson(composerFile).getAsJsonObject();
                    } catch (IOException e) {
                        continue;
                    }

                    JsonElement moduleName = jsonObject.get("name");//  .findProperty("name");

                    if (moduleName == null) {
                        continue;
                    }

                    System.out.println(moduleName.getAsString());
                    localRoots.put(moduleName.getAsString(), moduleDir.getCanonicalPath());
                }
            }

            return new JsonElementVisitor() {
                final private String basePath = project.getBasePath();
                final private Map<String, String> modules = new HashMap<>();

                @Override
                public void visitFile(PsiFile file) {
                    for (InstalledPackageData installedPackageData : ComposerConfigUtils.getInstalledPackagesFromConfig(file.getVirtualFile())) {
                        String packageName = installedPackageData.getName();
                        VirtualFile path = localFileSystem.findFileByPath(basePath + "/vendor/" + packageName);

                        if (path != null && path.exists()) {
                            // Compare this package with the packages found in the directory above
                        }
                    }
                }
            };
        }

        return PsiElementVisitor.EMPTY_VISITOR;
    }
}
