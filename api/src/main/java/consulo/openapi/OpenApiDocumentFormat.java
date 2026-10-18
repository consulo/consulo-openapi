package consulo.openapi;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public enum OpenApiDocumentFormat {
    YAML("yaml"),
    JSON("json");

    private final String myExtension;

    OpenApiDocumentFormat(String extension) {
        myExtension = extension;
    }

    public String getExtension() {
        return myExtension;
    }
}
