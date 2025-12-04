package de.hthoene.loralite.aitoolkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.hthoene.loralite.util.WorkspaceProperties;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Service
public class AiToolkitService {
    private final Path configFolderPath;
    private final Path logsFolderPath;
    private final Path aiToolkitFolderPath;
    private final ObjectMapper yamlMapper;

    public AiToolkitService(WorkspaceProperties workspaceProperties) {
        this.configFolderPath = workspaceProperties.getConfigsPath();
        this.logsFolderPath = workspaceProperties.getLogsPath();
        this.aiToolkitFolderPath = workspaceProperties.getAiToolkitPath();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public Path train(JobConfiguration config) throws IOException {
        Path latestConfigDir = configFolderPath.resolve("latest");
        Files.createDirectories(latestConfigDir);
        Path configFile = latestConfigDir.resolve("train.yaml");
        yamlMapper.writeValue(configFile.toFile(), config);

        Files.createDirectories(logsFolderPath);
        Path logFile = logsFolderPath.resolve("latest.txt");

        CompletableFuture
                .supplyAsync(() -> {
                    try (FileOutputStream logOut = new FileOutputStream(logFile.toFile(), true)) {
                        return new ProcessExecutor()
                                .directory(aiToolkitFolderPath.toFile())
                                .command("python", "run.py", configFile.toAbsolutePath().toString())
                                .redirectOutput(logOut)
                                .redirectError(logOut)
                                .readOutput(false)
                                .exitValues(0)
                                .execute();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .exceptionally(ex -> null);

        return logFile;
    }
}
