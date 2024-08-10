package de.greensurvivors.greenbook.bootstrap;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage") // plugin loader
public class GreenPluginLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build());

        resolver.addDependency(new Dependency(new DefaultArtifact("com.github.ben-manes.caffeine:caffeine:3.1.8"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.apache.commons:commons-collections4:4.5.0-M1"), null));
        classpathBuilder.addLibrary(resolver);

        classpathBuilder.getContext().getLogger().info("loaded libaries caffeine and commons-collections4.");
    }
}
