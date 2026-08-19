package model;

import java.util.ArrayList;
import java.util.List;
import util.Constants;

/**
 * Represents the user's preferences for the MailMate application.
 *
 * Settings control how MailMate behaves without storing
 * actual email content or account credentials.
 */
public class Settings {

    // =====================================================
    // USER PREFERENCES
    // =====================================================

    private final List<String> importantSenders;

    /*
     * Senders manually marked as spam by the user.
     *
     * These are email addresses, not email contents.
     */
    private final List<String> spamSenders;

    private int spamSensitivity;
    private boolean summaryEnabled;
    private boolean dailyBriefEnabled;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    /**
     * Constructs Settings with default preferences.
     */
    public Settings() {

        this.importantSenders =
                new ArrayList<>();

        this.spamSenders =
                new ArrayList<>();

        this.spamSensitivity =
                Constants.DEFAULT_SPAM_SENSITIVITY;

        this.summaryEnabled =
                true;

        this.dailyBriefEnabled =
                true;
    }


    // =====================================================
    // IMPORTANT SENDER MANAGEMENT
    // =====================================================

    /**
     * Adds an email address to the important sender list.
     *
     * @param emailAddress email address to mark as important
     */
    public void addImportantSender(
            String emailAddress) {

        String normalized =
                normalizeEmailAddress(
                        emailAddress
                );

        if (!normalized.isBlank()
                && !containsIgnoreCase(
                        importantSenders,
                        normalized
                )) {

            importantSenders.add(
                    normalized
            );
        }
    }


    /**
     * Removes an email address from the important
     * sender list.
     *
     * @param emailAddress email address to remove
     */
    public void removeImportantSender(
            String emailAddress) {

        removeIgnoreCase(
                importantSenders,
                emailAddress
        );
    }


    /**
     * Checks whether an email address is marked
     * as important.
     */
    public boolean isImportantSender(
            String emailAddress) {

        String normalized =
                normalizeEmailAddress(
                        emailAddress
                );

        return containsIgnoreCase(
                importantSenders,
                normalized
        );
    }


    /**
     * Returns the important sender list.
     *
     * A copy is returned so external code cannot
     * directly modify the internal settings state.
     */
    public List<String> getImportantSenders() {

        return new ArrayList<>(
                importantSenders
        );
    }


    // =====================================================
    // SPAM SENDER MANAGEMENT
    // =====================================================

    /**
     * Adds an email address to the manually
     * blocked spam sender list.
     *
     * Once a sender is manually marked as spam,
     * MailMate can classify future emails from that
     * address as spam.
     */
    public void addSpamSender(
            String emailAddress) {

        String normalized =
                normalizeEmailAddress(
                        emailAddress
                );

        if (!normalized.isBlank()
                && !containsIgnoreCase(
                        spamSenders,
                        normalized
                )) {

            spamSenders.add(
                    normalized
            );
        }
    }


    /**
     * Removes an email address from the manually
     * blocked spam sender list.
     */
    public void removeSpamSender(
            String emailAddress) {

        removeIgnoreCase(
                spamSenders,
                emailAddress
        );
    }


    /**
     * Checks whether a sender has been manually
     * marked as spam.
     */
    public boolean isSpamSender(
            String emailAddress) {

        String normalized =
                normalizeEmailAddress(
                        emailAddress
                );

        return containsIgnoreCase(
                spamSenders,
                normalized
        );
    }


    /**
     * Returns all manually blocked spam senders.
     *
     * A copy is returned to protect internal state.
     */
    public List<String> getSpamSenders() {

        return new ArrayList<>(
                spamSenders
        );
    }


    /**
     * Removes all manually blocked spam senders.
     */
    public void clearSpamSenders() {

        spamSenders.clear();
    }


    // =====================================================
    // SPAM SENSITIVITY
    // =====================================================

    public int getSpamSensitivity() {

        return spamSensitivity;
    }


    public void setSpamSensitivity(
            int spamSensitivity) {

        if (spamSensitivity >= 0
                && spamSensitivity <= 100) {

            this.spamSensitivity =
                    spamSensitivity;
        }
    }


    // =====================================================
    // SUMMARY SETTINGS
    // =====================================================

    public boolean isSummaryEnabled() {

        return summaryEnabled;
    }


    public void setSummaryEnabled(
            boolean summaryEnabled) {

        this.summaryEnabled =
                summaryEnabled;
    }


    // =====================================================
    // DAILY BRIEF SETTINGS
    // =====================================================

    public boolean isDailyBriefEnabled() {

        return dailyBriefEnabled;
    }


    public void setDailyBriefEnabled(
            boolean dailyBriefEnabled) {

        this.dailyBriefEnabled =
                dailyBriefEnabled;
    }


    // =====================================================
    // INTERNAL HELPERS
    // =====================================================

    /**
     * Normalizes an email address for comparison.
     */
    private String normalizeEmailAddress(
            String emailAddress) {

        if (emailAddress == null
                || emailAddress.isBlank()) {

            return "";
        }

        String normalized =
                emailAddress
                        .trim()
                        .toLowerCase();

        /*
         * Supports formats such as:
         *
         * user@example.com
         *
         * Person Name <user@example.com>
         */
        int start =
                normalized.indexOf('<');

        int end =
                normalized.indexOf('>');

        if (start >= 0
                && end > start) {

            normalized =
                    normalized.substring(
                            start + 1,
                            end
                    )
                    .trim();
        }

        return normalized;
    }


    /**
     * Performs case-insensitive list lookup.
     */
    private boolean containsIgnoreCase(
            List<String> list,
            String value) {

        if (list == null
                || value == null
                || value.isBlank()) {

            return false;
        }

        for (String item : list) {

            if (item != null
                    && item.equalsIgnoreCase(
                            value
                    )) {

                return true;
            }
        }

        return false;
    }


    /**
     * Removes a value from a list using
     * case-insensitive comparison.
     */
    private void removeIgnoreCase(
            List<String> list,
            String value) {

        String normalized =
                normalizeEmailAddress(
                        value
                );

        if (normalized.isBlank()) {
            return;
        }

        list.removeIf(
                item ->
                        item != null
                                && item.equalsIgnoreCase(
                                        normalized
                                )
        );
    }


    // =====================================================
    // STRING REPRESENTATION
    // =====================================================

    @Override
    public String toString() {

        return "Settings {" +
                "\n  Important Senders  : "
                + importantSenders.size() +
                "\n  Spam Senders       : "
                + spamSenders.size() +
                "\n  Spam Sensitivity   : "
                + spamSensitivity +
                "\n  Summary Enabled    : "
                + summaryEnabled +
                "\n  Daily Brief Enabled: "
                + dailyBriefEnabled +
                "\n}";
    }
}