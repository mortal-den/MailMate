package service;

import model.Email;

import java.util.ArrayList;
import java.util.List;

/**
 * Categorizes emails using deterministic local rules.
 */
public class CategoryService {

    public enum Category {
        WORK,
        PERSONAL,
        FINANCE,
        EDUCATION,
        SOCIAL,
        PROMOTIONAL,
        OTHER
    }


    /**
     * Determines the most appropriate category.
     */
    public Category categorize(
            Email email) {

        if (email == null) {

            return Category.OTHER;
        }

        /*
         * Spam is kept separate from normal categories.
         * We don't create a SPAM category because spam is
         * already represented by Email.isSpam().
         */
        if (email.isSpam()) {

            return Category.OTHER;
        }

        /*
         * Promotional mail gets its own category.
         */
        if (email.isPromotional()) {

            return Category.PROMOTIONAL;
        }

        String content =
                buildSearchableContent(
                        email
                );


        // =================================================
        // FINANCE
        // =================================================

        if (containsAny(
                content,
                "invoice",
                "payment",
                "bank",
                "transaction",
                "credit",
                "debit",
                "refund",
                "salary",
                "billing",
                "statement",
                "emi",
                "loan",
                "tax"
        )) {

            return Category.FINANCE;
        }


        // =================================================
        // EDUCATION
        // =================================================

        if (containsAny(
                content,
                "exam",
                "assignment",
                "lecture",
                "course",
                "college",
                "university",
                "professor",
                "class",
                "homework",
                "internship",
                "placement",
                "semester",
                "faculty",
                "student",
                "campus"
        )) {

            return Category.EDUCATION;
        }


        // =================================================
        // WORK
        // =================================================

        if (containsAny(
                content,
                "meeting",
                "project",
                "deadline",
                "office",
                "client",
                "manager",
                "employee",
                "company",
                "team",
                "work",
                "business",
                "proposal",
                "presentation"
        )) {

            return Category.WORK;
        }


        // =================================================
        // SOCIAL
        // =================================================

        if (containsAny(
                content,
                "facebook",
                "instagram",
                "linkedin",
                "twitter",
                "friend",
                "birthday",
                "party",
                "social",
                "community",
                "invitation"
        )) {

            return Category.SOCIAL;
        }


        // =================================================
        // PERSONAL
        // =================================================

        if (containsAny(
                content,
                "family",
                "mom",
                "mother",
                "dad",
                "father",
                "brother",
                "sister",
                "personal",
                "home"
        )) {

            return Category.PERSONAL;
        }


        return Category.OTHER;
    }


    /**
     * Categorizes multiple emails.
     */
    public List<Category> categorizeAll(
            List<Email> emails) {

        List<Category> categories =
                new ArrayList<>();

        if (emails == null) {

            return categories;
        }

        for (Email email :
                emails) {

            categories.add(
                    categorize(
                            email
                    )
            );
        }

        return categories;
    }


    /**
     * Returns emails belonging to a category.
     */
    public List<Email> findByCategory(
            List<Email> emails,
            Category category) {

        List<Email> results =
                new ArrayList<>();

        if (emails == null
                || category == null) {

            return results;
        }

        for (Email email :
                emails) {

            if (email == null) {
                continue;
            }

            if (categorize(email)
                    == category) {

                results.add(
                        email
                );
            }
        }

        return results;
    }


    // =====================================================
    // TEXT ANALYSIS
    // =====================================================

    private String buildSearchableContent(
            Email email) {

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

        return sender
                + " "
                + subject
                + " "
                + body;
    }


    private boolean containsAny(
            String content,
            String... keywords) {

        if (content == null) {

            return false;
        }

        for (String keyword :
                keywords) {

            if (keyword == null
                    || keyword.isBlank()) {

                continue;
            }

            if (content.contains(
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