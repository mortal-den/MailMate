package service;

import model.Email;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;


/**
 * Provides local search and filtering operations
 * over loaded MailMate emails.
 */
public class SearchService {

    // =====================================================
    // GENERAL SEARCH
    // =====================================================

    /**
     * Searches sender, receiver, subject and body.
     *
     * Search is case-insensitive.
     */
    public List<Email> search(
            List<Email> emails,
            String keyword) {

        if (emails == null) {

            return new ArrayList<>();
        }

        if (keyword == null
                || keyword.isBlank()) {

            return new ArrayList<>(
                    emails
            );
        }

        String searchTerm =
                keyword
                        .toLowerCase()
                        .trim();

        List<Email> results =
                new ArrayList<>();


        for (Email email :
                emails) {

            if (email == null) {
                continue;
            }

            if (containsIgnoreCase(
                    email.getSender(),
                    searchTerm
            )
                    || containsIgnoreCase(
                            email.getReceiver(),
                            searchTerm
                    )
                    || containsIgnoreCase(
                            email.getSubject(),
                            searchTerm
                    )
                    || containsIgnoreCase(
                            email.getBody(),
                            searchTerm
                    )) {

                results.add(
                        email
                );
            }
        }

        return results;
    }


    // =====================================================
    // SENDER
    // =====================================================

    public List<Email> searchBySender(
            List<Email> emails,
            String sender) {

        if (emails == null) {

            return new ArrayList<>();
        }

        if (sender == null
                || sender.isBlank()) {

            return new ArrayList<>();
        }

        return filterByText(
                emails,
                sender,
                SearchField.SENDER
        );
    }


    // =====================================================
    // SUBJECT
    // =====================================================

    public List<Email> searchBySubject(
            List<Email> emails,
            String subject) {

        if (emails == null) {

            return new ArrayList<>();
        }

        if (subject == null
                || subject.isBlank()) {

            return new ArrayList<>();
        }

        return filterByText(
                emails,
                subject,
                SearchField.SUBJECT
        );
    }


    // =====================================================
    // DATE
    // =====================================================

    /**
     * Searches emails received between two dates.
     *
     * Null start/end values are treated as open bounds.
     */
    public List<Email> searchByDateRange(
            List<Email> emails,
            LocalDateTime start,
            LocalDateTime end) {

        List<Email> results =
                new ArrayList<>();

        if (emails == null) {

            return results;
        }


        for (Email email :
                emails) {

            if (email == null
                    || email.getReceivedDate() == null) {

                continue;
            }

            LocalDateTime received =
                    email.getReceivedDate();


            boolean afterStart =
                    start == null
                            || !received.isBefore(
                                    start
                            );


            boolean beforeEnd =
                    end == null
                            || !received.isAfter(
                                    end
                            );


            if (afterStart
                    && beforeEnd) {

                results.add(
                        email
                );
            }
        }

        return results;
    }


    // =====================================================
    // READ / UNREAD
    // =====================================================

    public List<Email> searchUnread(
            List<Email> emails) {

        return filterByBoolean(
                emails,
                BooleanField.READ,
                false
        );
    }


    public List<Email> searchRead(
            List<Email> emails) {

        return filterByBoolean(
                emails,
                BooleanField.READ,
                true
        );
    }


    // =====================================================
    // STARRED
    // =====================================================

    public List<Email> searchStarred(
            List<Email> emails) {

        return filterByBoolean(
                emails,
                BooleanField.STARRED,
                true
        );
    }


    // =====================================================
    // IMPORTANT
    // =====================================================

    public List<Email> searchImportant(
            List<Email> emails) {

        return filterByBoolean(
                emails,
                BooleanField.IMPORTANT,
                true
        );
    }


    // =====================================================
    // SPAM
    // =====================================================

    public List<Email> searchSpam(
            List<Email> emails) {

        return filterByBoolean(
                emails,
                BooleanField.SPAM,
                true
        );
    }


    // =====================================================
    // PROMOTIONAL
    // =====================================================

    public List<Email> searchPromotional(
            List<Email> emails) {

        return filterByBoolean(
                emails,
                BooleanField.PROMOTIONAL,
                true
        );
    }


    // =====================================================
    // ATTACHMENTS
    // =====================================================

    public List<Email> searchWithAttachments(
            List<Email> emails) {

        return filterByBoolean(
                emails,
                BooleanField.ATTACHMENT,
                true
        );
    }


    // =====================================================
    // CATEGORY
    // =====================================================

    /**
     * Filters by the category already calculated
     * and stored in Email.
     *
     * Comparison is case-insensitive.
     */
    public List<Email> searchByCategory(
            List<Email> emails,
            String category) {

        List<Email> results =
                new ArrayList<>();

        if (emails == null
                || category == null
                || category.isBlank()) {

            return results;
        }

        String target =
                category
                        .trim()
                        .toLowerCase();


        for (Email email :
                emails) {

            if (email == null) {
                continue;
            }

            String currentCategory =
                    email.getCategory();

            if (currentCategory != null
                    && currentCategory
                            .toLowerCase()
                            .equals(
                                    target
                            )) {

                results.add(
                        email
                );
            }
        }

        return results;
    }


    // =====================================================
    // TRUST SCORE
    // =====================================================

    /**
     * Returns emails whose stored trust score
     * meets the requested minimum.
     */
    public List<Email> searchByMinimumTrustScore(
            List<Email> emails,
            int minimumScore) {

        List<Email> results =
                new ArrayList<>();

        if (emails == null) {

            return results;
        }

        int normalizedMinimum =
                Math.max(
                        0,
                        Math.min(
                                100,
                                minimumScore
                        )
                );


        for (Email email :
                emails) {

            if (email == null) {
                continue;
            }

            if (email.getTrustScore()
                    >= normalizedMinimum) {

                results.add(
                        email
                );
            }
        }

        return results;
    }


    // =====================================================
    // PRIVATE FILTER TYPES
    // =====================================================

    private enum SearchField {
        SENDER,
        SUBJECT
    }


    private enum BooleanField {
        READ,
        STARRED,
        IMPORTANT,
        SPAM,
        PROMOTIONAL,
        ATTACHMENT
    }


    // =====================================================
    // PRIVATE TEXT FILTER
    // =====================================================

    private List<Email> filterByText(
            List<Email> emails,
            String searchTerm,
            SearchField field) {

        List<Email> results =
                new ArrayList<>();

        if (emails == null
                || searchTerm == null
                || searchTerm.isBlank()) {

            return results;
        }

        String normalized =
                searchTerm
                        .toLowerCase()
                        .trim();


        for (Email email :
                emails) {

            if (email == null) {
                continue;
            }

            String value;

            if (field == SearchField.SENDER) {

                value =
                        email.getSender();

            } else {

                value =
                        email.getSubject();
            }


            if (containsIgnoreCase(
                    value,
                    normalized
            )) {

                results.add(
                        email
                );
            }
        }

        return results;
    }


    // =====================================================
    // PRIVATE BOOLEAN FILTER
    // =====================================================

    private List<Email> filterByBoolean(
            List<Email> emails,
            BooleanField field,
            boolean expected) {

        List<Email> results =
                new ArrayList<>();

        if (emails == null) {

            return results;
        }


        for (Email email :
                emails) {

            if (email == null) {
                continue;
            }

            boolean actual;


            switch (field) {

                case READ:

                    actual =
                            email.isRead();

                    break;


                case STARRED:

                    actual =
                            email.isStarred();

                    break;


                case IMPORTANT:

                    actual =
                            email.isImportant();

                    break;


                case SPAM:

                    actual =
                            email.isSpam();

                    break;


                case PROMOTIONAL:

                    actual =
                            email.isPromotional();

                    break;


                case ATTACHMENT:

                    actual =
                            email.hasAttachment();

                    break;


                default:

                    actual =
                            false;
            }


            if (actual == expected) {

                results.add(
                        email
                );
            }
        }

        return results;
    }


    // =====================================================
    // TEXT MATCHING
    // =====================================================

    private boolean containsIgnoreCase(
            String text,
            String searchTerm) {

        if (text == null
                || searchTerm == null) {

            return false;
        }

        return text
                .toLowerCase()
                .contains(
                        searchTerm
                                .toLowerCase()
                );
    }
}