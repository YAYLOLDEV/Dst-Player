package io.lolyay.musicPlayer;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

public class PLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder builder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addDependency(dep("io.github.jaredmdobson:concentus:1.0.2"));
        resolver.addDependency(dep("io.lolyay.dct:client:8.0.6")); //TODO will change a lot

        // ─── REPOSITORIES ───
        resolver.addRepository(new RemoteRepository.Builder(
                "lolyay", "default", "https://maven.lolyay.dev/releases"
        ).build());

        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build());

        builder.addLibrary(resolver);
    }
    private static Dependency dep(String coords) {
        return new Dependency(new DefaultArtifact(coords), null);
    }
}
