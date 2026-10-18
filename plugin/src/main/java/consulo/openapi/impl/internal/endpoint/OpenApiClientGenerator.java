package consulo.openapi.impl.internal.endpoint;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.client.generator.ClientExample;
import consulo.endpoint.client.generator.ClientGenerator;
import consulo.endpoint.oas.OpenApiSpecification;
import consulo.localize.LocalizeValue;
import consulo.openapi.OpenApiDocumentFormat;
import consulo.openapi.OpenApiDocumentWriter;
import consulo.openapi.localize.OpenApiLocalize;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public final class OpenApiClientGenerator implements ClientGenerator {
    @Override
    public LocalizeValue getTitle() {
        return OpenApiLocalize.clientGeneratorTitle();
    }

    @Override
    public @Nullable ClientExample generate(OpenApiSpecification openApiSpecification) {
        if (!openApiSpecification.getPaths().iterator().hasNext()) {
            return null;
        }
        OpenApiDocumentFormat format = OpenApiDocumentFormat.YAML;
        String text = OpenApiDocumentWriter.write(openApiSpecification, format, OpenApiLocalize.defaultDocumentTitle().get());
        return ClientExample.fromFileExtension(text, format.getExtension());
    }
}
