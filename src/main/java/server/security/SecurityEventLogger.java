package server.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SecurityEventLogger {
    private static final Path LOG_DIR = Path.of("logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("security-events.log");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SecurityEventLogger() {
    }

    public static synchronized void log(String eventType, String username, String ipAddress, String action, String status) {
        try {
            Files.createDirectories(LOG_DIR);
            String line = String.format(
                    "%s | user=%s | ip=%s | type=%s | action=%s | status=%s%n",
                    LocalDateTime.now().format(FORMATTER),
                    safe(username),
                    safe(ipAddress),
                    safe(eventType),
                    safe(action),
                    safe(status)
            );
            Files.writeString(
                    LOG_FILE,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Unable to write security log: " + e.getMessage());
        }
    }

    public static void alert(String username, String ipAddress, String action, String status) {
        log("ALERT", username, ipAddress, action, status);
        System.err.println("[SECURITY ALERT] ip=" + safe(ipAddress) + " user=" + safe(username)
                + " action=" + safe(action) + " status=" + safe(status));
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replace('|', '/');
    }
}
