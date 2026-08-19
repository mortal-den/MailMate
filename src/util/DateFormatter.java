package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides date and time formatting utilities for MailMate.
 */
public final class DateFormatter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT);

    private DateFormatter() {
        // Prevent object creation.
    }

    /**
     * Formats a LocalDateTime into MailMate's standard date/time format.
     *
     * @param dateTime Date and time to format
     * @return Formatted date/time string
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        return dateTime.format(FORMATTER);
    }

    /**
     * Converts a formatted date/time string back into LocalDateTime.
     *
     * @param dateTimeString Formatted date/time string
     * @return Parsed LocalDateTime
     * @throws IllegalArgumentException if the string cannot be parsed
     */
    public static LocalDateTime parse(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            throw new IllegalArgumentException("Date/time value cannot be blank.");
        }

        return LocalDateTime.parse(dateTimeString, FORMATTER);
    }
}