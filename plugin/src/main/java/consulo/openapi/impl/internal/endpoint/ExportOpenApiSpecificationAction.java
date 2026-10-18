package consulo.openapi.impl.internal.endpoint;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.concurrent.coroutine.DisposableCoroutineScope;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.component.ProcessCanceledException;
import consulo.endpoint.EndpointDataKeys;
import consulo.endpoint.EndpointListItem;
import consulo.endpoint.client.generator.ClientGenerator;
import consulo.endpoint.client.generator.ClientGeneratorOpenInScratchService;
import consulo.endpoint.oas.OasExportUtil;
import consulo.endpoint.oas.OpenApiSpecification;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.openapi.localize.OpenApiLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ActionImpl(id = "OpenApi.ExportSpecificationFromEndpoints", parents = @ActionParentRef(@ActionRef(id = "Endpoints.ContextMenu")))
public final class ExportOpenApiSpecificationAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private static final Logger LOG = Logger.getInstance(ExportOpenApiSpecificationAction.class);

    public ExportOpenApiSpecificationAction() {
        super(OpenApiLocalize.actionExportSpecificationText(), LocalizeValue.empty(), PlatformIconGroup.actionsExport());
    }

    @Override
    public void update(AnActionEvent e) {
        List<EndpointListItem> items = e.getData(EndpointDataKeys.SELECTED_ITEMS);
        e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null && items != null && !items.isEmpty());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        List<EndpointListItem> items = e.getData(EndpointDataKeys.SELECTED_ITEMS);
        if (project == null || items == null || items.isEmpty()) {
            return;
        }
        ClientGenerator generator = project.getExtensionPoint(ClientGenerator.class).findExtension(OpenApiClientGenerator.class);
        if (generator == null) {
            return;
        }
        List<EndpointListItem> selected = List.copyOf(items);
        ClientGeneratorOpenInScratchService service = ClientGeneratorOpenInScratchService.getInstance(project);
        DisposableCoroutineScope.launchAsync(
            project.coroutineContext(),
            project,
            () -> Coroutine
                .first(ReadLock.<Object, Optional<OpenApiSpecification>>apply(ignored -> computeSpecification(selected)))
                .then(CompletableFutureStep.<Optional<OpenApiSpecification>, Object>await(specification -> {
                    if (specification.isEmpty()) {
                        return CompletableFuture.<Object>completedFuture(specification);
                    }
                    OpenApiSpecification openApiSpecification = specification.get();
                    return service.createScratchFile(generator, openApiSpecification)
                        .<Object>thenApply(ignored -> openApiSpecification)
                        .whenComplete((ignored, error) -> {
                            if (error != null) {
                                LOG.error(error);
                            }
                        });
                }))
        );
    }

    @RequiredReadAction
    private static Optional<OpenApiSpecification> computeSpecification(List<EndpointListItem> selected) {
        try {
            return Optional.ofNullable(OasExportUtil.getOpenApiSpecification(selected));
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
            return Optional.empty();
        }
    }
}
