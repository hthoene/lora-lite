package de.hthoene.loralite.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ArchiveService {
    private static final DateTimeFormatter ARCHIVE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path configFolderPath;
    private final Path datasetFolderPath;
    private final Path outputFolderPath;
    private final Path logsFolderPath;
    private final Path archiveBasePath;

    public ArchiveService(WorkspaceProperties workspaceProperties) {
        this.configFolderPath = workspaceProperties.getConfigsPath();
        this.datasetFolderPath = workspaceProperties.getDatasetPath();
        this.outputFolderPath = workspaceProperties.getOutputPath();
        this.logsFolderPath = workspaceProperties.getLogsPath();
        this.archiveBasePath = workspaceProperties.getArchivePath();
    }

    public void archiveAllLatestFolders() throws IOException {
        String timestamp = LocalDateTime.now().format(ARCHIVE_FORMATTER);
        Path archiveFolder = archiveBasePath.resolve(timestamp);
        Files.createDirectories(archiveFolder);

        archiveLatestFolder(configFolderPath, archiveFolder.resolve("configs"));
        archiveLatestFolder(datasetFolderPath, archiveFolder.resolve("dataset"));
        archiveLatestFolder(outputFolderPath, archiveFolder.resolve("output"));
        archiveLatestLogFile(archiveFolder.resolve("logs.txt"));

        log.info("Archived latest workflow to {}", archiveFolder);
    }

    private void archiveLatestFolder(Path baseFolder, Path targetFolder) throws IOException {
        Path latestFolder = baseFolder.resolve("latest");

        if (!Files.exists(latestFolder)) {
            log.debug("Latest folder does not exist: {}", latestFolder);
            return;
        }

        if (!hasContent(latestFolder)) {
            log.debug("Latest folder is empty, skipping: {}", latestFolder);
            return;
        }

        Path parent = targetFolder.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        copyDirectory(latestFolder, targetFolder);
        deleteDirectory(latestFolder);
        Files.createDirectories(latestFolder);

        log.info("Archived {} to {}", latestFolder, targetFolder);
    }

    private void archiveLatestLogFile(Path targetFile) throws IOException {
        Path latestLogFile = logsFolderPath.resolve("latest.txt");

        if (!Files.exists(latestLogFile)) {
            log.debug("Latest log file does not exist: {}", latestLogFile);
            return;
        }

        if (Files.size(latestLogFile) == 0) {
            log.debug("Latest log file is empty, skipping: {}", latestLogFile);
            return;
        }

        Path parent = targetFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.copy(latestLogFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(latestLogFile);
        Files.createFile(latestLogFile);

        log.info("Archived {} to {}", latestLogFile, targetFile);
    }

    private boolean hasContent(Path folder) throws IOException {
        try (var stream = Files.list(folder)) {
            return stream.findAny().isPresent();
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
