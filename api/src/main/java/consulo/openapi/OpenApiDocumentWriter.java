package consulo.openapi;

import consulo.endpoint.oas.OpenApiSpecification;
import consulo.openapi.internal.OpenApiDocumentTree;
import consulo.openapi.internal.OpenApiJsonEmitter;
import consulo.openapi.internal.OpenApiYamlEmitter;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class OpenApiDocumentWriter {
    public static final String DEFAULT_VERSION = "1.0.0";

    private OpenApiDocumentWriter() {
    }

    public static String write(OpenApiSpecification specification, OpenApiDocumentFormat format, String title) {
        return write(specification, format, title, DEFAULT_VERSION);
    }

    public static String write(OpenApiSpecification specification, OpenApiDocumentFormat format, String title, String version) {
        Map<String, Object> document = OpenApiDocumentTree.build(specification, title, version);
        return switch (format) {
            case YAML -> OpenApiYamlEmitter.emit(document);
            case JSON -> OpenApiJsonEmitter.emit(document);
        };
    }
}
