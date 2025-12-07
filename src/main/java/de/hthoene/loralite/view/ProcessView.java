package de.hthoene.loralite.view;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.hthoene.loralite.aitoolkit.AiToolkitService;
import de.hthoene.loralite.component.LogPanel;
import de.hthoene.loralite.util.GpuInfo;
import de.hthoene.loralite.util.GpuMonitor;
import de.hthoene.loralite.util.GpuStats;
import de.hthoene.loralite.util.WorkspaceProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

@Slf4j
public class ProcessView extends VerticalLayout {

    private static final String[] ALLOWED_IMAGE_FILE_TYPES = { "png", "jpg", "jpeg", "webp" };

    private final GpuMonitor gpuMonitor;
    private final Path outputDirectory;
    private final LogPanel logPanel;
    private final AiToolkitService aiToolkitService;

    private final FlexLayout samplesLayout = new FlexLayout();

    private final ProgressBar gpuUtilBar = new ProgressBar(0, 100, 0);
    private final Span gpuUtilLabel = new Span("GPU Util: -");

    private final ProgressBar gpuMemBar = new ProgressBar(0, 100, 0);
    private final Span gpuMemLabel = new Span("GPU Mem: -");

    private final Button cancelButton;

    private int lastSampleCount = 0;

    public ProcessView(GpuMonitor gpuMonitor, LogPanel logPanel, WorkspaceProperties workspaceProperties, AiToolkitService aiToolkitService) {
        this.gpuMonitor = gpuMonitor;
        this.logPanel = logPanel;
        this.outputDirectory = workspaceProperties.getOutputPath().resolve("latest");
        this.aiToolkitService = aiToolkitService;

        ensureOutputDirectoryExists();

        setPadding(true);
        setSpacing(true);

        cancelButton =
                new com.vaadin.flow.component.button.Button("Stop training", e -> {
                    aiToolkitService.cancelCurrent();
                    logPanel.log("Training cancelled by user.");
                });

        cancelButton.setEnabled(aiToolkitService.isRunning());

        gpuUtilBar.setWidth(300, Unit.PIXELS);
        gpuMemBar.setWidth(300, Unit.PIXELS);

        gpuUtilLabel.setWidth("256px");
        gpuMemLabel.setWidth("256px");

        setupSamplesLayout();

        HorizontalLayout gpuUtilLayout = new HorizontalLayout(gpuUtilLabel, gpuUtilBar);
        gpuUtilLayout.setWidthFull();
        gpuUtilLayout.setAlignItems(Alignment.CENTER);

        HorizontalLayout gpuMemLayout = new HorizontalLayout(gpuMemLabel, gpuMemBar);
        gpuMemLayout.setWidthFull();
        gpuMemLayout.setAlignItems(Alignment.CENTER);

        VerticalLayout gpuInner = new VerticalLayout(gpuUtilLayout, gpuMemLayout);
        gpuInner.setWidth("520px");
        gpuInner.setPadding(false);
        gpuInner.setSpacing(false);
        gpuInner.setAlignItems(Alignment.START);

        VerticalLayout gpuOuter = new VerticalLayout(gpuInner);
        gpuOuter.setWidthFull();
        gpuOuter.setPadding(false);
        gpuOuter.setAlignItems(Alignment.CENTER);

        setAlignItems(Alignment.CENTER);

        add(cancelButton, gpuOuter, samplesLayout);
    }


    private void ensureOutputDirectoryExists() {
        if (!outputDirectory.toFile().exists()) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (IOException e) {
                logPanel.log(e);
                log.warn("Could not create output directory {}", outputDirectory, e);
            }
        }
    }

    private void setupSamplesLayout() {
        samplesLayout.setWidthFull();
        samplesLayout.getStyle().set("overflow-x", "auto");
        samplesLayout.getStyle().set("box-sizing", "border-box");
        samplesLayout.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        samplesLayout.getStyle().set("padding", "1rem");
        samplesLayout.getStyle().set("border-radius", "0.5rem");
        samplesLayout.getStyle().set("gap", "0.5rem");
        samplesLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    }

    public void refresh() {
        refreshSamples();
        refreshGpuStats();
        cancelButton.setEnabled(aiToolkitService.isRunning());
    }

    private void refreshGpuStats() {
        GpuStats stats = gpuMonitor.getLastStats();
        if (stats == null || stats.getGpus() == null || stats.getGpus().isEmpty()) {
            gpuUtilLabel.setText("GPU Util: -");
            gpuMemLabel.setText("GPU Mem: -");
            gpuUtilBar.setValue(0);
            gpuMemBar.setValue(0);
            return;
        }

        GpuInfo gpu0 = stats.getGpus().getFirst();
        double util = gpu0.getUtil();
        long used = gpu0.getMem_used();
        long total = gpu0.getMem_total();

        gpuUtilLabel.setText("GPU Util: " + String.format("%.1f%%", util));
        gpuMemLabel.setText("GPU Mem: " + used + " / " + total + " MB");

        double utilClamped = Math.clamp(util, 0, 100);
        double memPercent = (total > 0) ? (used * 100.0 / total) : 0.0;
        double memClamped = Math.clamp(memPercent, 0, 100);

        gpuUtilBar.setValue(utilClamped);
        gpuMemBar.setValue(memClamped);
    }

    private void refreshSamples() {
        Path samplesDir = outputDirectory.resolve("samples");
        File dir = samplesDir.toFile();
        File[] files = dir.exists() ? dir.listFiles() : null;

        int currentCount = (files == null) ? 0 :
                (int) Arrays.stream(files)
                        .filter(File::isFile)
                        .filter(this::isImageFile)
                        .count();

        if (currentCount == lastSampleCount) {
            return;
        }
        lastSampleCount = currentCount;

        samplesLayout.removeAll();

        if (currentCount == 0) {
            samplesLayout.add(new Span("No samples yet"));
            return;
        }

        Arrays.stream(files)
                .filter(File::isFile)
                .filter(this::isImageFile)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .forEach(this::addSampleThumbnail);
    }

    private boolean isImageFile(File f) {
        String name = f.getName().toLowerCase(Locale.ROOT);
        return Arrays.stream(ALLOWED_IMAGE_FILE_TYPES)
                .anyMatch(ext -> name.endsWith("." + ext));
    }

    private void addSampleThumbnail(File imageFile) {
        Path imagePath = imageFile.toPath();

        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            try {
                String mimeType = Files.probeContentType(imagePath);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                return new DownloadResponse(
                        Files.newInputStream(imagePath),
                        imageFile.getName(),
                        mimeType,
                        Files.size(imagePath)
                );
            } catch (IOException e) {
                logPanel.log(e);
                event.getResponse().setStatus(500);
                return DownloadResponse.error(500);
            }
        });

        Image img = new Image(handler, imageFile.getName());
        img.setMaxHeight("15rem");
        img.getStyle().set("object-fit", "contain");
        img.getStyle().set("border-radius", "0.5rem");
        img.getStyle().set("cursor", "pointer");

        img.addClickListener(e -> openFullImageDialog(handler, imageFile));

        samplesLayout.add(img);
    }

    private void openFullImageDialog(DownloadHandler handler, File imageFile) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        Image full = new Image(handler, imageFile.getName());
        full.getStyle().set("max-width", "90vw");
        full.getStyle().set("max-height", "90vh");
        full.getStyle().set("display", "block");
        full.getStyle().set("margin", "auto");

        dialog.add(full);
        dialog.open();
    }
}
