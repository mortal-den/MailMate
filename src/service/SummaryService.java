package service;

import model.Email;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates short local summaries of emails.
 *
 * Version 1 is completely local and does not send
 * email content to any external AI service.
 */
public class SummaryService {

    private static final int MAX_SUMMARY_LENGTH = 300;

    private static final int MAX_SENTENCES = 3;


    /**
     * Creates a short summary of an email.
     */
    public String summarize(
            Email email) {

        if (email == null) {

            return "";
        }

        String body =
                email.getBody();

        if (body == null
                || body.isBlank()) {

            return "No email content available.";
        }

        String cleanedText =
                cleanText(
                        body
                );

        if (cleanedText.isBlank()) {

            return "No email content available.";
        }

        /*
         * Short messages can be returned directly.
         */
        if (cleanedText.length()
                <= MAX_SUMMARY_LENGTH) {

            return cleanedText;
        }

        List<String> sentences =
                splitIntoSentences(
                        cleanedText
                );

        StringBuilder summary =
                new StringBuilder();

        for (String sentence :
                sentences) {

            if (sentence.isBlank()) {
                continue;
            }

            String candidate;

            if (summary.isEmpty()) {

                candidate =
                        sentence;

            } else {

                candidate =
                        summary
                                + " "
                                + sentence;
            }

            if (candidate.length()
                    > MAX_SUMMARY_LENGTH) {

                break;
            }

            summary.setLength(0);

            summary.append(
                    candidate
            );

            if (countSentences(
                    summary.toString()
            ) >= MAX_SENTENCES) {

                break;
            }
        }

        /*
         * If sentence extraction was not useful,
         * use character-based fallback.
         */
        if (summary.isEmpty()) {

            return createShortSummary(
                    cleanedText
            );
        }

        String result =
                summary.toString()
                        .trim();

        if (result.length()
                < cleanedText.length()) {

            result += "...";
        }

        return result;
    }


    /**
     * Generates a summary that includes the subject.
     */
    public String summarizeWithSubject(
            Email email) {

        if (email == null) {

            return "";
        }

        String subject =
                cleanText(
                        email.getSubject()
                );

        String summary =
                summarize(
                        email
                );

        if (subject.isBlank()) {

            return summary;
        }

        if (summary.isBlank()) {

            return subject;
        }

        String combined =
                subject
                        + ": "
                        + summary;

        if (combined.length()
                <= MAX_SUMMARY_LENGTH) {

            return combined;
        }

        return createShortSummary(
                combined
        );
    }


    /**
     * Generates summaries for a list of emails.
     */
    public List<String> summarizeAll(
            List<Email> emails) {

        List<String> summaries =
                new ArrayList<>();

        if (emails == null) {

            return summaries;
        }

        for (Email email :
                emails) {

            summaries.add(
                    summarize(
                            email
                    )
            );
        }

        return summaries;
    }


    // =====================================================
    // TEXT PROCESSING
    // =====================================================

    private String cleanText(
            String text) {

        if (text == null) {

            return "";
        }

        return text
                .replaceAll(
                        "\\r\\n|\\r|\\n",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }


    private List<String> splitIntoSentences(
            String text) {

        List<String> sentences =
                new ArrayList<>();

        if (text == null
                || text.isBlank()) {

            return sentences;
        }

        String[] parts =
                text.split(
                        "(?<=[.!?])\\s+"
                );

        for (String part :
                parts) {

            String sentence =
                    part.trim();

            if (!sentence.isBlank()) {

                sentences.add(
                        sentence
                );
            }

            if (sentences.size()
                    >= MAX_SENTENCES) {

                break;
            }
        }

        /*
         * Emails without sentence punctuation
         * become one unit.
         */
        if (sentences.isEmpty()) {

            sentences.add(
                    text.trim()
            );
        }

        return sentences;
    }


    private String createShortSummary(
            String text) {

        if (text == null
                || text.isBlank()) {

            return "No email content available.";
        }

        if (text.length()
                <= MAX_SUMMARY_LENGTH) {

            return text;
        }

        String shortened =
                text.substring(
                        0,
                        MAX_SUMMARY_LENGTH
                );

        int lastSpace =
                shortened.lastIndexOf(' ');

        if (lastSpace > 0) {

            shortened =
                    shortened.substring(
                            0,
                            lastSpace
                    );
        }

        return shortened.trim()
                + "...";
    }


    private int countSentences(
            String text) {

        if (text == null
                || text.isBlank()) {

            return 0;
        }

        int count = 0;

        for (int i = 0;
             i < text.length();
             i++) {

            char character =
                    text.charAt(i);

            if (character == '.'
                    || character == '!'
                    || character == '?') {

                count++;
            }
        }

        return count;
    }
}