package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Application logger for MailMate.
 *
 * The logger is intentionally designed to record application
 * events and errors without storing or displaying private
 * email content, OAuth tokens, credentials, or message bodies.
 */
public final class Logger {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    private Logger() {
        // Utility class.
    }

    /**
     * Logs a normal application event.
     */
    public static void info(String message) {

        write("INFO", message);
    }

    /**
     * Logs a warning.
     */
    public static void warning(String message) {

        write("WARNING", message);
    }

    /**
     * Logs an error message.
     */
    public static void error(String message) {

        write("ERROR", message);
    }

    /**
     * Logs an error together with its exception.
     *
     * The exception message is included, but sensitive
     * email content should never be passed to this method.
     */
    public static void error(
            String message,
            Exception exception) {

        write(
                "ERROR",
                message
                        + " - "
                        + sanitizeExceptionMessage(
                                exception
                        )
        );
    }

    /**
     * Writes the actual log entry.
     */
    private static void write(
            String level,
            String message) {

        String timestamp =
                LocalDateTime.now()
                        .format(TIME_FORMAT);

        System.out.println(
                "["
                        + timestamp
                        + "] ["
                        + level
                        + "] "
                        + safeMessage(message)
        );
    }

    /**
     * Prevents null log messages.
     */
    private static String safeMessage(
            String message) {

        if (message == null
                || message.isBlank()) {

            return "(no log message)";
        }

        return message;
    }

    /**
     * Keeps exception logging reasonably safe.
     *
     * Application code should still avoid passing
     * sensitive data inside exception messages.
     */
    private static String sanitizeExceptionMessage(
            Exception exception) {

        if (exception == null) {
            return "(unknown error)";
        }

        String message =
                exception.getMessage();

        if (message == null
                || message.isBlank()) {

            return exception
                    .getClass()
                    .getSimpleName();
        }

        return exception
                .getClass()
                .getSimpleName()
                + ": "
                + message;
    }
}