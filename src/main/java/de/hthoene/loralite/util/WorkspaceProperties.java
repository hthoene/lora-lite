package de.hthoene.loralite.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
@ConfigurationProperties(prefix = "loralite.folder")
public class WorkspaceProperties {
    private String configs = "/workspace/configs";
    private String dataset = "/workspace/dataset";
    private String output = "/workspace/output";
    private String logs = "/workspace/logs";
    private String archive = "/workspace/archive";
    private String templates = "/workspace/templates";
    private String monitor = "/workspace/monitor";
    private String aiToolkit = "/workspace/ai-toolkit";

    public Path getConfigsPath() {
        return Path.of(configs);
    }

    public Path getDatasetPath() {
        return Path.of(dataset);
    }

    public Path getOutputPath() {
        return Path.of(output);
    }

    public Path getLogsPath() {
        return Path.of(logs);
    }

    public Path getArchivePath() {
        return Path.of(archive);
    }

    public Path getTemplatesPath() {
        return Path.of(templates);
    }

    public Path getMonitorPath() {
        return Path.of(monitor);
    }

    public Path getAiToolkitPath() {
        return Path.of(aiToolkit);
    }

    public String getConfigs() {
        return configs;
    }

    public void setConfigs(String configs) {
        this.configs = configs;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }

    public String getArchive() {
        return archive;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public String getTemplates() {
        return templates;
    }

    public void setTemplates(String templates) {
        this.templates = templates;
    }

    public String getMonitor() {
        return monitor;
    }

    public void setMonitor(String monitor) {
        this.monitor = monitor;
    }

    public String getAiToolkit() {
        return aiToolkit;
    }

    public void setAiToolkit(String aiToolkit) {
        this.aiToolkit = aiToolkit;
    }
}
