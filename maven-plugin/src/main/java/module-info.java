/**
 * @author VISTALL
 * @since 2025-10-04
 */
module consulo.openapi.maven {
    requires consulo.ide.api;

    requires consulo.component.api;
    requires consulo.application.content.api;
    requires consulo.language.api;
    requires consulo.module.api;

    requires org.jetbrains.idea.maven;
    requires org.jetbrains.idea.maven.server.common;
}
