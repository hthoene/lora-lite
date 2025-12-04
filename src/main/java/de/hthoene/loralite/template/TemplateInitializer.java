package de.hthoene.loralite.template;

import de.hthoene.loralite.util.WorkspaceProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class TemplateInitializer implements ApplicationRunner {
    private final Path targetDir;

    public TemplateInitializer(WorkspaceProperties workspaceProperties) {
        this.targetDir = workspaceProperties.getTemplatesPath();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(targetDir);

        try (var stream = Files.list(targetDir)) {
            if (stream.findAny().isPresent()) {
                return;
            }
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        copyPattern(resolver, "classpath:configurations/*.yaml");
        copyPattern(resolver, "classpath:configurations/*.yml");
    }

    private void copyPattern(PathMatchingResourcePatternResolver resolver, String pattern)
            throws IOException {
        Resource[] resources = resolver.getResources(pattern);
        for (Resource resource : resources) {
            if (!resource.isReadable()) {
                continue;
            }
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }

            Path target = targetDir.resolve(filename);
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
