package de.hthoene.loralite.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.hthoene.loralite.component.LogPanel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class LogView extends VerticalLayout {
    private final LogPanel logPanel;
    private Thread tailThread;

    public LogView(LogPanel logPanel) {
        this.logPanel = logPanel;

        setSizeFull();
        setPadding(false);
        setFlexGrow(1, logPanel);

        add(logPanel);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        File logFile = logPanel.getLogFile();

        long startOffset = 0L;
        try {
            String all = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
            if (!all.isBlank()) {
                logPanel.appendToUi(all);
                startOffset = logFile.length();
            } else {
                startOffset = logFile.length();
            }
        } catch (IOException e) {
            log.warn("Could not read log file", e);
            startOffset = logFile.length();
        }

        final long initialOffset = startOffset;
        tailThread = new Thread(() -> tailLogFile(ui, logFile, initialOffset), "log-tail-thread");
        tailThread.setDaemon(true);
        tailThread.start();
    }

    private void tailLogFile(UI ui, File logFile, long initialOffset) {
        long lastSize = initialOffset;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                long size = logFile.length();

                if (size < lastSize) {
                    lastSize = 0L;
                }

                if (size > lastSize) {
                    String newContent = readFromOffset(logFile.toPath(), lastSize);
                    lastSize = size;

                    if (!newContent.isBlank()) {
                        ui.access(() -> logPanel.appendToUi(newContent));
                    }
                }

                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                log.warn("Could not read log file", e);
                break;
            }
        }
    }

    private static String readFromOffset(Path path, long offset) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = raf.length();
            if (offset >= fileLength) {
                return "";
            }
            raf.seek(offset);

            long delta = fileLength - offset;
            int toRead = (int) Math.min(delta, 64 * 1024);
            byte[] buffer = new byte[toRead];
            int read = raf.read(buffer);
            if (read <= 0) {
                return "";
            }
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (tailThread != null) {
            tailThread.interrupt();
            tailThread = null;
        }
    }
}
