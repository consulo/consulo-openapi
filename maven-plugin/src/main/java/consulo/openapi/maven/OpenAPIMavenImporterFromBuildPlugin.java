package consulo.openapi.maven;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.maven.importing.MavenImporterFromBuildPlugin;
import consulo.maven.rt.server.common.model.MavenPlugin;
import consulo.module.Module;
import jakarta.annotation.Nonnull;
import org.jdom.Element;
import org.jetbrains.idea.maven.importing.MavenContentFolder;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2025-10-01
 */
@ExtensionImpl
public class OpenAPIMavenImporterFromBuildPlugin extends MavenImporterFromBuildPlugin {
    public OpenAPIMavenImporterFromBuildPlugin() {
        super("org.openapitools", "openapi-generator-maven-plugin");
    }

    @Override
    public void preProcess(Module module, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, MavenModifiableModelsProvider mavenModifiableModelsProvider) {

    }

    @Override
    public void process(MavenModifiableModelsProvider mavenModifiableModelsProvider,
                        Module module,
                        MavenRootModelAdapter mavenRootModelAdapter,
                        MavenProjectsTree mavenProjectsTree,
                        MavenProject mavenProject,
                        MavenProjectChanges mavenProjectChanges,
                        Map<MavenProject, String> map,
                        List<MavenProjectsProcessorTask> list) {

    }

    @Override
    public void collectContentFolders(MavenProject mavenProject, BiFunction<ContentFolderTypeProvider, String, MavenContentFolder> folderAcceptor) {
        MavenPlugin plugin = mavenProject.findPlugin(myPluginGroupID, myPluginArtifactID);
        if (plugin == null) {
            return;
        }

        String srcPath = Objects.requireNonNullElse(getCustomSourceRoot(plugin), "src/main/java");

        Path generatedSources = Path.of(mavenProject.getDirectory(), "target/generated-sources/openapi/" + srcPath);
        if (Files.exists(generatedSources)) {
            folderAcceptor.apply(ProductionContentFolderTypeProvider.getInstance(), generatedSources.toString()).setGenerated();
        }
    }

    private String getCustomSourceRoot(MavenPlugin plugin) {
        Element generate = plugin.getGoalConfiguration("generate");
        if (generate == null) {
            return null;
        }

        Element configOptions = generate.getChild("configOptions");
        if (configOptions == null) {
            return null;
        }
        return configOptions.getChildTextTrim("sourceFolder");
    }

    @Override
    public boolean isExcludedGenerationSourceFolder(@Nonnull MavenProject mavenProject, @Nonnull String sourcePath, @Nonnull ContentFolderTypeProvider typeProvider) {
        Path generatedSources = Path.of(mavenProject.getDirectory(), "target/generated-sources/openapi/");
        Path currentDir = Path.of(sourcePath);

        if (generatedSources.equals(currentDir)) {
            return true;
        }
        return super.isExcludedGenerationSourceFolder(mavenProject, sourcePath, typeProvider);
    }
}
