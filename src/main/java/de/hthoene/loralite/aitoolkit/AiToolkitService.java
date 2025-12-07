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

    private volatile Process currentProcess;

    public AiToolkitService(WorkspaceProperties workspaceProperties) {
        this.configFolderPath = workspaceProperties.getConfigsPath();
        this.logsFolderPath = workspaceProperties.getLogsPath();
        this.aiToolkitFolderPath = workspaceProperties.getAiToolkitPath();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public synchronized Path train(JobConfiguration config) throws IOException {
        if (currentProcess != null && currentProcess.isAlive()) {
            throw new IllegalStateException("A training process is already running");
        }

        Path latestConfigDir = configFolderPath.resolve("latest");
        Files.createDirectories(latestConfigDir);
        Path configFile = latestConfigDir.resolve("train.yaml");
        yamlMapper.writeValue(configFile.toFile(), config);

        Files.createDirectories(logsFolderPath);
        Path logFile = logsFolderPath.resolve("latest.txt");

        CompletableFuture
                .supplyAsync(() -> {
                    try (FileOutputStream logOut = new FileOutputStream(logFile.toFile(), true)) {
                        Process process = new ProcessExecutor()
                                .directory(aiToolkitFolderPath.toFile())
                                .command("python", "run.py", configFile.toAbsolutePath().toString())
                                .redirectOutput(logOut)
                                .redirectError(logOut)
                                .readOutput(false)
                                .destroyOnExit()
                                .start()
                                .getProcess();

                        synchronized (AiToolkitService.this) {
                            currentProcess = process;
                        }

                        int exitCode = process.waitFor();

                        synchronized (AiToolkitService.this) {
                            currentProcess = null;
                        }

                        return exitCode;
                    } catch (Exception e) {
                        synchronized (AiToolkitService.this) {
                            currentProcess = null;
                        }
                        return null;
                    }
                })
                .exceptionally(ex -> null);

        return logFile;
    }

    public synchronized void cancelCurrent() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
            try {
                if (!currentProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    currentProcess.destroyForcibly();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                currentProcess = null;
            }
        }
    }

    public synchronized boolean isRunning() {
        return currentProcess != null && currentProcess.isAlive();
    }
}
