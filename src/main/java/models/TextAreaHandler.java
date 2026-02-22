package models;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class TextAreaHandler extends Handler {
    private TextArea textArea;
    private final List<String> logBuffer = new ArrayList<>();
    private static final int MAX_BUFFER_SIZE = 1000; // Prevent memory issues

    public TextAreaHandler() {
        setFormatter(new SimpleFormatter());
    }

    public void setTextArea(TextArea textArea) {
        this.textArea = textArea;
        // When TextArea is attached, dump all buffered logs into it
        if (textArea != null && !logBuffer.isEmpty()) {
            Platform.runLater(() -> {
                for (String log : logBuffer) {
                    textArea.appendText(log);
                }
                textArea.setScrollTop(Double.MAX_VALUE);
            });
        }
    }

    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record)) {
            String formattedMessage = getFormatter().format(record);

            // Always add to buffer
            logBuffer.add(formattedMessage);

            // Limit buffer size to prevent memory issues
            if (logBuffer.size() > MAX_BUFFER_SIZE) {
                logBuffer.removeFirst();
            }

            // If TextArea is attached, also display immediately
            if (textArea != null) {
                Platform.runLater(() -> {
                    textArea.appendText(formattedMessage);
                    textArea.setScrollTop(Double.MAX_VALUE);
                });
            }
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {
        logBuffer.clear();
    }
}