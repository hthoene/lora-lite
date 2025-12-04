package de.hthoene.loralite.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.hthoene.loralite.aitoolkit.JobConfiguration;
import de.hthoene.loralite.util.WorkspaceProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class TemplateService {

    private final ObjectMapper mapper;
    private final Path templateDir;

    public TemplateService(WorkspaceProperties workspaceProperties) {
        this.mapper = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
        this.templateDir = workspaceProperties.getTemplatesPath();
    }

    public List<String> listTemplateNames() {
        try (Stream<Path> stream = Files.list(templateDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".yaml"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JobConfiguration loadTemplate(String fileName) {
        Path path = templateDir.resolve(fileName);
        try (InputStream in = Files.newInputStream(path)) {
            return mapper.readValue(in, JobConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
