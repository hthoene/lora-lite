package de.hthoene.loralite.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class GpuStatsService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path monitorScript;

    public GpuStatsService(WorkspaceProperties workspaceProperties) {
        this.monitorScript = workspaceProperties.getMonitorPath().resolve("gpu_monitor.py");
    }

    public Optional<GpuStats> queryGpuStats() {
        try {
            Process process = new ProcessBuilder("python", monitorScript.toString())
                    .redirectErrorStream(true)
                    .start();

            String json;
            try (InputStream is = process.getInputStream()) {
                json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || json.isBlank()) {
                return Optional.empty();
            }

            GpuStats stats = objectMapper.readValue(json, GpuStats.class);
            return Optional.of(stats);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
