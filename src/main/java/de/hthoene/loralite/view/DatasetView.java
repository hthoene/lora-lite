package de.hthoene.loralite.view;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import de.hthoene.loralite.component.DatasetEntry;
import de.hthoene.loralite.component.LogPanel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
public class DatasetView extends VerticalLayout {
    private static final String[] ALLOWED_IMAGE_FILE_TYPES = { "png", "jpg", "jpeg", "webp" };

    private final LogPanel logPanel;
    private final Path datasetPath;
    private final VerticalLayout datasetLayout = new VerticalLayout();

    public DatasetView(LogPanel logPanel, Environment environment) {
        this.logPanel = logPanel;

        setAlignItems(Alignment.CENTER);

        this.datasetPath = Paths
                .get(environment.getProperty("loralite.folder.dataset", "/workspace/dataset"))
                .resolve("latest");
        createDatasetDirectory();

        Upload datasetUpload = createUploadComponent();
        add(datasetUpload);

        datasetLayout.setAlignItems(Alignment.CENTER);
        add(datasetLayout);

        updateDatasetPresentation();
    }

    private void createDatasetDirectory() {
        try {
            Files.createDirectories(datasetPath);
        } catch (IOException e) {
            log.error("Could not create directory {}", datasetPath, e);
            logPanel.log(e);
            throw new RuntimeException(e);
        }
    }

    private Upload createUploadComponent() {
        Upload datasetUpload = new Upload();
        datasetUpload.setDropLabel(new Span("Select or drop images or text files"));
        datasetUpload.setAcceptedFileTypes(
                "image/jpeg", "image/jpg", "image/png", "image/webp", "text/plain"
        );

        datasetUpload.setUploadHandler(event -> {
            try {
                Files.createDirectories(datasetPath);
                Path outputPath = datasetPath.resolve(event.getFileName());
                Files.copy(event.getInputStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logPanel.log(e);
            }
        });

        datasetUpload.addAllFinishedListener(event -> {
            datasetUpload.clearFileList();
            updateDatasetPresentation();
        });

        return datasetUpload;
    }

    public void updateDatasetPresentation() {
        datasetLayout.removeAll();

        File[] files = datasetPath.toFile().listFiles();
        if (files == null || files.length == 0) {
            datasetLayout.add(new Span("No dataset files yet"));
            return;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            boolean isImage = FilenameUtils.isExtension(file.getName(), ALLOWED_IMAGE_FILE_TYPES);
            if (!isImage) {
                continue;
            }

            String baseName = FilenameUtils.getBaseName(file.getName());
            File captionFile = datasetPath.resolve(baseName + ".txt").toFile();
            ensureCaptionFileExists(captionFile);

            DatasetEntry datasetEntry =
                    new DatasetEntry(file, captionFile, logPanel, refresh -> updateDatasetPresentation());
            datasetLayout.add(datasetEntry);
        }
    }

    private void ensureCaptionFileExists(File captionFile) {
        if (captionFile.exists()) {
            return;
        }
        try {
            Files.createFile(captionFile.toPath());
        } catch (IOException e) {
            logPanel.log(e);
        }
    }
}
