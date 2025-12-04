package de.hthoene.loralite.view.flux;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hthoene.loralite.util.WorkspaceProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class UiSettingsService {
    private final ObjectMapper mapper;
    private final Path settingsFile;

    public UiSettingsService(WorkspaceProperties workspaceProperties) {
        this.mapper = new ObjectMapper().findAndRegisterModules();
        this.settingsFile = workspaceProperties
                .getConfigsPath()
                .resolve("settings.json");
    }

    public UiSettings load() {
        try {
            if (Files.exists(settingsFile)) {
                return mapper.readValue(settingsFile.toFile(), UiSettings.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new UiSettings();
    }

    public void save(UiSettings settings) {
        try {
            Path parent = settingsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(settingsFile.toFile(), settings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
