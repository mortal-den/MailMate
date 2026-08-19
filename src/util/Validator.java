package util;

/**
 * Provides reusable validation methods for the MailMate application.
 *
 * This class contains only validation logic and does not store
 * any application state.
 */
public final class Validator {

    private Validator() {
        // Prevent object creation.
    }

    // =====================================================
    // Email Validation
    // =====================================================

    /**
     * Checks whether a string has a basic valid email format.
     *
     * @param emailAddress Email address to validate
     * @return true if the format is valid, otherwise false
     */
    public static boolean isValidEmail(String emailAddress) {

        if (emailAddress == null || emailAddress.isBlank()) {
            return false;
        }

        String emailPattern =
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        return emailAddress.matches(emailPattern);
    }

    // =====================================================
    // Text Validation
    // =====================================================

    /**
     * Checks whether a string is null or contains only whitespace.
     *
     * @param value String to check
     * @return true if null or blank, otherwise false
     */
    public static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Checks whether a required piece of text has been provided.
     *
     * @param value Text to validate
     * @return true if the text contains meaningful content
     */
    public static boolean isValidText(String value) {
        return value != null && !value.isBlank();
    }

    // =====================================================
    // Spam Sensitivity Validation
    // =====================================================

    /**
     * Checks whether a spam sensitivity value is within
     * the allowed range of 0 to 100.
     *
     * @param sensitivity Spam sensitivity value
     * @return true if the value is between 0 and 100
     */
    public static boolean isValidSpamSensitivity(int sensitivity) {
        return sensitivity >= 0 && sensitivity <= 100;
    }
}