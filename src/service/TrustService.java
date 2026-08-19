package service;

import model.Email;

/**
 * Calculates a local trust score for an email.
 *
 * Score range:
 *
 * 0   = very low trust
 * 100 = very high trust
 */
public class TrustService {

    private static final int MIN_TRUST_SCORE = 0;
    private static final int MAX_TRUST_SCORE = 100;

    private static final int TRUSTED_THRESHOLD = 70;
    private static final int MEDIUM_THRESHOLD = 40;


    /**
     * Calculates the trust score.
     *
     * No email data is sent to an external service.
     */
    public int calculateTrustScore(
            Email email) {

        if (email == null) {

            return MIN_TRUST_SCORE;
        }

        int score = 50;

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
        // SENDER FORMAT
        // =================================================

        String senderAddress =
                extractEmailAddress(
                        sender
                );

        if (senderAddress.isBlank()) {

            score -= 25;

        } else if (isValidEmailAddress(
                senderAddress
        )) {

            score += 15;

        } else {

            score -= 15;
        }


        // =================================================
        // SUSPICIOUS SENDER
        // =================================================

        if (containsAny(
                sender,
                "winner",
                "prize",
                "free-money",
                "free_money",
                "claim",
                "lottery",
                "security-alert",
                "account-alert",
                "urgent"
        )) {

            score -= 20;
        }


        // =================================================
        // SUSPICIOUS SUBJECT
        // =================================================

        if (containsAny(
                subject,
                "you won",
                "claim now",
                "urgent action",
                "verify immediately",
                "free money",
                "account suspended",
                "account blocked",
                "last warning",
                "your account will be closed"
        )) {

            score -= 15;
        }


        // =================================================
        // SUSPICIOUS CONTENT
        // =================================================

        if (containsAny(
                content,
                "send money",
                "send payment",
                "bank details",
                "credit card details",
                "wire transfer",
                "claim your prize",
                "you have won",
                "verify your identity",
                "click here to verify",
                "click here to confirm",
                "login immediately",
                "password expired",
                "account will be deleted"
        )) {

            score -= 20;
        }


        // =================================================
        // SPAM
        // =================================================

        if (email.isSpam()) {

            score -= 30;
        }


        // =================================================
        // PROMOTIONAL
        // =================================================

        /*
         * Promotional mail is not automatically dangerous.
         * Only a small reduction is applied.
         */
        if (email.isPromotional()) {

            score -= 5;
        }


        // =================================================
        // FINAL SCORE
        // =================================================

        return clampScore(
                score
        );
    }


    /**
     * Determines whether an email has high trust.
     */
    public boolean isTrusted(
            Email email) {

        return calculateTrustScore(
                email
        ) >= TRUSTED_THRESHOLD;
    }


    /**
     * Returns HIGH, MEDIUM or LOW.
     */
    public String getTrustLevel(
            Email email) {

        int score =
                calculateTrustScore(
                        email
                );

        if (score >= TRUSTED_THRESHOLD) {

            return "HIGH";
        }

        if (score >= MEDIUM_THRESHOLD) {

            return "MEDIUM";
        }

        return "LOW";
    }


    /**
     * Returns a user-friendly description.
     */
    public String getTrustDescription(
            Email email) {

        String level =
                getTrustLevel(
                        email
                );

        return switch (level) {

            case "HIGH" ->
                    "High trust";

            case "MEDIUM" ->
                    "Moderate trust";

            default ->
                    "Low trust";
        };
    }


    // =====================================================
    // SENDER
    // =====================================================

    private String extractEmailAddress(
            String sender) {

        if (sender == null
                || sender.isBlank()) {

            return "";
        }

        int start =
                sender.indexOf('<');

        int end =
                sender.indexOf('>');

        if (start >= 0
                && end > start) {

            return sender
                    .substring(
                            start + 1,
                            end
                    )
                    .trim();
        }

        return sender.trim();
    }


    private boolean isValidEmailAddress(
            String emailAddress) {

        if (emailAddress == null
                || emailAddress.isBlank()) {

            return false;
        }

        int atIndex =
                emailAddress.indexOf('@');

        int lastAtIndex =
                emailAddress.lastIndexOf('@');

        int dotIndex =
                emailAddress.lastIndexOf('.');

        return atIndex > 0
                && atIndex == lastAtIndex
                && dotIndex > atIndex + 1
                && dotIndex < emailAddress.length() - 1;
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


    private int clampScore(
            int score) {

        return Math.max(
                MIN_TRUST_SCORE,
                Math.min(
                        MAX_TRUST_SCORE,
                        score
                )
        );
    }
}