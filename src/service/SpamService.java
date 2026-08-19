package service;

import model.Email;
import model.Settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpamService {

    /**
     * Minimum score required for automatic spam detection.
     */
    private static final int SPAM_THRESHOLD = 3;

    /**
     * User settings containing manually blocked
     * spam senders.
     */
    private final Settings settings;


    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    /**
     * Creates a SpamService with its own default settings.
     *
     * Kept for compatibility with existing code such as
     * BriefService.
     */
    public SpamService() {

        this(
                new Settings()
        );
    }


    /**
     * Creates a SpamService using the supplied
     * application Settings object.
     *
     * This is the preferred constructor when MailMate
     * owns the application settings.
     */
    public SpamService(
            Settings settings) {

        if (settings == null) {

            throw new IllegalArgumentException(
                    "Settings cannot be null."
            );
        }

        this.settings =
                settings;
    }


    // =====================================================
    // SPAM SCORE
    // =====================================================

    /**
     * Calculates an automatic spam score.
     *
     * No email data is sent to an external service.
     */
    public int calculateSpamScore(
            Email email) {

        if (email == null) {
            return 0;
        }

        int score = 0;

        String sender =
                safeText(
                        email.getSender()
                );

        String subject =
                safeText(
                        email.getSubject()
                );

        String body =
                safeText(
                        email.getBody()
                );

        String content =
                sender
                        + " "
                        + subject
                        + " "
                        + body;


        // =================================================
        // SUSPICIOUS SUBJECT
        // =================================================

        if (containsAny(
                subject,
                "urgent",
                "you won",
                "winner",
                "claim now",
                "act now",
                "account suspended",
                "account blocked",
                "verify immediately",
                "password expired",
                "security alert",
                "last warning"
        )) {

            score++;
        }


        // =================================================
        // SUSPICIOUS CONTENT
        // =================================================

        if (containsAny(
                content,
                "free money",
                "guaranteed profit",
                "claim your prize",
                "you have won",
                "send money",
                "verify your account",
                "send payment",
                "bank details",
                "credit card details",
                "wire transfer",
                "click here to verify",
                "click here to confirm",
                "verify your identity",
                "confirm your identity",
                "login immediately"
        )) {

            score++;
        }


        // =================================================
        // SUSPICIOUS SENDER PATTERNS
        // =================================================

        if (containsAny(
                sender,
                "winner",
                "prize",
                "lottery",
                "security-alert",
                "account-alert",
                "free-money"
        )) {

            score++;
        }


        // =================================================
        // EXCESSIVE LINKS
        // =================================================

        /*
         * Promotional mail often contains links, so
         * this rule intentionally requires many links.
         */
        if (countLinks(body) > 8) {

            score++;
        }


        // =================================================
        // PHISHING LANGUAGE
        // =================================================

        if (containsAny(
                content,
                "your account will be closed",
                "your account will be deleted",
                "your account has been suspended",
                "confirm your identity now",
                "click here immediately",
                "login to prevent closure"
        )) {

            score++;
        }

        return score;
    }


    // =====================================================
    // SPAM DETECTION
    // =====================================================

    /**
     * Determines whether an email is spam.
     *
     * Manual sender rules have highest priority.
     */
    public boolean isSpam(
            Email email) {

        if (email == null) {
            return false;
        }

        String sender =
                extractEmailAddress(
                        email.getSender()
                );

        /*
         * If the user explicitly marked this sender
         * as spam, always classify the email as spam.
         */
        if (isSenderMarkedAsSpam(
                sender
        )) {

            return true;
        }

        /*
         * Promotional mail is not automatically spam.
         */
        if (isPromotional(
                email
        )) {

            return false;
        }

        return calculateSpamScore(
                email
        ) >= SPAM_THRESHOLD;
    }


    /**
     * Returns all detected spam emails.
     */
    public List<Email> findSpam(
            List<Email> emails) {

        List<Email> spamEmails =
                new ArrayList<>();

        if (emails == null) {
            return spamEmails;
        }

        for (Email email :
                emails) {

            if (email != null
                    && isSpam(email)) {

                spamEmails.add(
                        email
                );
            }
        }

        return spamEmails;
    }


    // =====================================================
    // PROMOTIONAL DETECTION
    // =====================================================

    /**
     * Determines whether an email appears promotional.
     *
     * Promotional emails are not automatically spam.
     */
    public boolean isPromotional(
            Email email) {

        if (email == null) {
            return false;
        }

        String sender =
                safeText(
                        email.getSender()
                );

        String subject =
                safeText(
                        email.getSubject()
                );

        String body =
                safeText(
                        email.getBody()
                );

        String content =
                sender
                        + " "
                        + subject
                        + " "
                        + body;

        return containsAny(
                content,
                "unsubscribe",
                "promotion",
                "promotional",
                "discount",
                "special offer",
                "limited time offer",
                "sale",
                "deal",
                "deals",
                "coupon",
                "newsletter",
                "shopping",
                "offer ends",
                "save 20%",
                "save 30%",
                "save 50%"
        );
    }


    /**
     * Returns all emails identified as promotional
     * but not spam.
     */
    public List<Email> findPromotional(
            List<Email> emails) {

        List<Email> promotionalEmails =
                new ArrayList<>();

        if (emails == null) {
            return promotionalEmails;
        }

        for (Email email :
                emails) {

            if (email != null
                    && isPromotional(email)
                    && !isSpam(email)) {

                promotionalEmails.add(
                        email
                );
            }
        }

        return promotionalEmails;
    }


    /**
     * Returns emails that are neither spam
     * nor promotional.
     */
    public List<Email> findNonSpam(
            List<Email> emails) {

        List<Email> nonSpamEmails =
                new ArrayList<>();

        if (emails == null) {
            return nonSpamEmails;
        }

        for (Email email :
                emails) {

            if (email != null
                    && !isSpam(email)
                    && !isPromotional(email)) {

                nonSpamEmails.add(
                        email
                );
            }
        }

        return nonSpamEmails;
    }


    // =====================================================
    // MANUAL "MARK AS SPAM"
    // =====================================================

    /**
     * Marks the sender of an email as spam.
     *
     * The sender is stored inside Settings.
     *
     * Therefore:
     *
     * Current email → spam
     * Future emails from sender → spam
     */
    public void markAsSpam(
            Email email) {

        if (email == null) {
            return;
        }

        String sender =
                extractEmailAddress(
                        email.getSender()
                );

        if (sender.isBlank()) {
            return;
        }

        settings.addSpamSender(
                sender
        );

        email.setSpam(
                true
        );

        email.setPromotional(
                false
        );
    }


    /**
     * Removes a sender from the manual spam list.
     */
    public void unmarkSenderAsSpam(
            String sender) {

        settings.removeSpamSender(
                extractEmailAddress(
                        sender
                )
        );
    }


    /**
     * Checks whether a sender has been manually
     * marked as spam.
     */
    public boolean isSenderMarkedAsSpam(
            String sender) {

        return settings.isSpamSender(
                extractEmailAddress(
                        sender
                )
        );
    }


    /**
     * Returns a copy of all manually blocked
     * spam senders.
     */
    public Set<String> getManuallyBlockedSenders() {

        return new HashSet<>(
                settings.getSpamSenders()
        );
    }


    /**
     * Clears all manually blocked spam senders.
     */
    public void clearManuallyBlockedSenders() {

        settings.clearSpamSenders();
    }


    /**
     * Returns the Settings object used by this service.
     */
    public Settings getSettings() {

        return settings;
    }


    // =====================================================
    // EMAIL ADDRESS HELPERS
    // =====================================================

    /**
     * Extracts the actual email address from a Gmail
     * "From" header.
     *
     * Example:
     *
     * Person Name <person@example.com>
     *
     * becomes:
     *
     * person@example.com
     */
    public String extractEmailAddress(
            String sender) {

        if (sender == null
                || sender.isBlank()) {

            return "";
        }

        String cleaned =
                sender
                        .trim()
                        .toLowerCase();

        int start =
                cleaned.indexOf('<');

        int end =
                cleaned.indexOf('>');

        if (start >= 0
                && end > start) {

            return cleaned
                    .substring(
                            start + 1,
                            end
                    )
                    .trim();
        }

        return cleaned;
    }


    // =====================================================
    // LINK ANALYSIS
    // =====================================================

    /**
     * Counts HTTP and HTTPS links in an email body.
     */
    private int countLinks(
            String text) {

        if (text == null
                || text.isBlank()) {

            return 0;
        }

        String lowerText =
                text.toLowerCase();

        int count = 0;
        int position = 0;

        while (position < lowerText.length()) {

            int httpPosition =
                    lowerText.indexOf(
                            "http://",
                            position
                    );

            int httpsPosition =
                    lowerText.indexOf(
                            "https://",
                            position
                    );

            int nextPosition;

            if (httpPosition == -1) {

                nextPosition =
                        httpsPosition;

            } else if (httpsPosition == -1) {

                nextPosition =
                        httpPosition;

            } else {

                nextPosition =
                        Math.min(
                                httpPosition,
                                httpsPosition
                        );
            }

            if (nextPosition == -1) {
                break;
            }

            count++;

            position =
                    nextPosition + 7;
        }

        return count;
    }


    // =====================================================
    // TEXT HELPERS
    // =====================================================

    private boolean containsAny(
            String text,
            String... keywords) {

        if (text == null) {
            return false;
        }

        for (String keyword :
                keywords) {

            if (keyword == null
                    || keyword.isBlank()) {

                continue;
            }

            if (text.contains(
                    keyword.toLowerCase()
            )) {

                return true;
            }
        }

        return false;
    }


    private String safeText(
            String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .trim();
    }
}