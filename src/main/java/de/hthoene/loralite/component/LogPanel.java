package de.hthoene.loralite.component;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.hthoene.loralite.util.WorkspaceProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

@Slf4j
public class LogPanel extends VerticalLayout {

    private static final Pattern ANSI_PATTERN =
            Pattern.compile("(\\u001B\\[[0-?]*[ -/]*[@-~])");

    private final TextArea logArea = new TextArea();
    private final File logFile;

    private String lastLine;

    public LogPanel(WorkspaceProperties workspaceProperties) throws IOException {
        Path logFolderPath = workspaceProperties.getLogsPath();
        Files.createDirectories(logFolderPath);

        File currentLogFile = logFolderPath.resolve("latest.txt").toFile();
        if (!currentLogFile.exists()) {
            Files.createDirectories(currentLogFile.toPath().getParent());
            Files.createFile(currentLogFile.toPath());
        }

        this.logFile = currentLogFile;
        initializeLayout();
    }

    private void initializeLayout() {
        setMinHeight("0");
        setPadding(false);

        logArea.setSizeFull();
        logArea.setMinHeight("0");
        logArea.setTitle("Logs");
        logArea.setReadOnly(true);

        add(logArea);
    }

    public File getLogFile() {
        return logFile;
    }

    public void appendToUi(String message) {
        String cleaned = stripAnsi(message);
        if (cleaned.isBlank()) {
            return;
        }

        String[] lines = cleaned.split("\\R");
        StringBuilder toAppend = new StringBuilder();

        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (line.equals(lastLine)) {
                continue;
            }
            if (toAppend.length() > 0) {
                toAppend.append("\n");
            }
            toAppend.append(line);
            lastLine = line;
        }

        if (toAppend.length() == 0) {
            return;
        }

        String current = logArea.getValue();
        if (current == null || current.isBlank()) {
            logArea.setValue(toAppend.toString());
        } else {
            logArea.setValue(current + "\n" + toAppend);
        }
        logArea.scrollToEnd();
    }

    public void log(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        log(sw.toString());
    }

    public void log(String message) {
        String cleaned = stripAnsi(message);
        if (cleaned.isBlank()) {
            return;
        }
        try {
            String toWrite = cleaned.endsWith("\n") ? cleaned : cleaned + "\n";
            Files.writeString(
                    logFile.toPath(),
                    toWrite,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            log.warn("Could not write to log file", e);
        }
    }

    public void clearUi() {
        logArea.setValue("");
        lastLine = null;
    }

    private static String stripAnsi(String s) {
        if (s == null) {
            return "";
        }
        String cleaned = ANSI_PATTERN.matcher(s).replaceAll("");
        cleaned = cleaned.replace("\r", "\n");
        return cleaned;
    }
}
