package service;

import model.Email;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Generates local MailMate email briefings.
 */
public class BriefService {

    private static final int MAX_PRIORITY_EMAILS = 5;


    private final SummaryService summaryService;
    private final CategoryService categoryService;
    private final TrustService trustService;
    private final SpamService spamService;


    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    /**
     * Compatibility constructor.
     *
     * Creates independent service instances.
     */
    public BriefService() {

        this(
                new SummaryService(),
                new CategoryService(),
                new TrustService(),
                new SpamService()
        );
    }


    /**
     * Allows MailMate to supply shared service instances.
     */
    public BriefService(
            SummaryService summaryService,
            CategoryService categoryService,
            TrustService trustService,
            SpamService spamService) {

        if (summaryService == null
                || categoryService == null
                || trustService == null
                || spamService == null) {

            throw new IllegalArgumentException(
                    "BriefService dependencies cannot be null."
            );
        }

        this.summaryService =
                summaryService;

        this.categoryService =
                categoryService;

        this.trustService =
                trustService;

        this.spamService =
                spamService;
    }


    // =====================================================
    // MAIN BRIEF
    // =====================================================

    /**
     * Creates a short briefing from a list of emails.
     *
     * No email content is sent externally.
     */
    public String generateBrief(
            List<Email> emails) {

        if (emails == null
                || emails.isEmpty()) {

            return "No emails available for the briefing.";
        }

        int totalEmails = 0;
        int unreadEmails = 0;
        int spamEmails = 0;
        int promotionalEmails = 0;
        int importantEmails = 0;
        int starredEmails = 0;

        List<Email> priorityEmails =
                new ArrayList<>();


        // =================================================
        // ANALYSIS
        // =================================================

        for (Email email :
                emails) {

            if (email == null) {
                continue;
            }

            totalEmails++;


            if (!email.isRead()) {

                unreadEmails++;
            }


            boolean spam =
                    spamService.isSpam(
                            email
                    );

            boolean promotional =
                    spamService.isPromotional(
                            email
                    );


            if (spam) {

                spamEmails++;

                email.setSpam(
                        true
                );

            } else if (promotional) {

                promotionalEmails++;

                email.setPromotional(
                        true
                );
            }


            if (email.isImportant()) {

                importantEmails++;
            }


            if (email.isStarred()) {

                starredEmails++;
            }


            /*
             * Spam and promotional emails are not included
             * as priority emails.
             */
            if (!spam
                    && !promotional
                    && isPriorityEmail(email)) {

                priorityEmails.add(
                        email
                );
            }
        }


        // =================================================
        // SORT PRIORITY EMAILS
        // =================================================

        priorityEmails.sort(
                Comparator
                        .comparingInt(
                                this::getPriorityScore
                        )
                        .reversed()
                        .thenComparing(
                                Email::getReceivedDate,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
        );


        // =================================================
        // BUILD BRIEF
        // =================================================

        StringBuilder brief =
                new StringBuilder();


        brief.append(
                "===== MAILMATE BRIEF =====\n\n"
        );


        brief.append(
                "Email Overview\n"
        );

        brief.append(
                "-------------------------\n"
        );

        brief.append(
                "Total emails       : "
        )
        .append(
                totalEmails
        )
        .append(
                "\n"
        );


        brief.append(
                "Unread emails      : "
        )
        .append(
                unreadEmails
        )
        .append(
                "\n"
        );


        brief.append(
                "Important          : "
        )
        .append(
                importantEmails
        )
        .append(
                "\n"
        );


        brief.append(
                "Starred            : "
        )
        .append(
                starredEmails
        )
        .append(
                "\n"
        );


        brief.append(
                "Possible spam      : "
        )
        .append(
                spamEmails
        )
        .append(
                "\n"
        );


        brief.append(
                "Promotional        : "
        )
        .append(
                promotionalEmails
        )
        .append(
                "\n\n"
        );


        // =================================================
        // PRIORITY SECTION
        // =================================================

        brief.append(
                "Priority Emails\n"
        );

        brief.append(
                "-------------------------\n"
        );


        if (priorityEmails.isEmpty()) {

            brief.append(
                    "No priority emails found.\n"
            );

        } else {

            int limit =
                    Math.min(
                            priorityEmails.size(),
                            MAX_PRIORITY_EMAILS
                    );


            for (int i = 0;
                 i < limit;
                 i++) {

                Email email =
                        priorityEmails.get(
                                i
                        );

                appendPriorityEmail(
                        brief,
                        email,
                        i + 1
                );
            }
        }


        brief.append(
                "\n=========================="
        );

        return brief.toString();
    }


    // =====================================================
    // UNREAD BRIEF
    // =====================================================

    public String generateUnreadBrief(
            List<Email> emails) {

        if (emails == null
                || emails.isEmpty()) {

            return "No unread emails available.";
        }

        List<Email> unreadEmails =
                new ArrayList<>();

        for (Email email :
                emails) {

            if (email != null
                    && !email.isRead()) {

                unreadEmails.add(
                        email
                );
            }
        }

        if (unreadEmails.isEmpty()) {

            return "No unread emails available.";
        }

        return generateBrief(
                unreadEmails
        );
    }


    // =====================================================
    // PRIORITY
    // =====================================================

    private boolean isPriorityEmail(
            Email email) {

        if (email == null) {

            return false;
        }

        /*
         * Explicit user flags always make a message
         * relevant to the priority section.
         */
        if (email.isImportant()
                || email.isStarred()) {

            return true;
        }


        /*
         * Unread + moderate/high trust is also useful.
         */
        if (!email.isRead()) {

            return trustService
                    .calculateTrustScore(
                            email
                    ) >= 60;
        }

        return false;
    }


    private int getPriorityScore(
            Email email) {

        int score = 0;

        if (email.isImportant()) {

            score += 30;
        }

        if (email.isStarred()) {

            score += 20;
        }

        if (!email.isRead()) {

            score += 15;
        }

        score +=
                trustService
                        .calculateTrustScore(
                                email
                        ) / 10;

        return score;
    }


    // =====================================================
    // PRIORITY EMAIL FORMAT
    // =====================================================

    private void appendPriorityEmail(
            StringBuilder brief,
            Email email,
            int position) {

        brief.append(
                "\n"
        );


        brief.append(
                position
        )
        .append(
                ". "
        )
        .append(
                safeText(
                        email.getSubject()
                )
        )
        .append(
                "\n"
        );


        brief.append(
                "   From: "
        )
        .append(
                safeText(
                        email.getSender()
                )
        )
        .append(
                "\n"
        );


        CategoryService.Category category =
                categoryService.categorize(
                        email
                );

        email.setCategory(
                category.name()
        );


        brief.append(
                "   Category: "
        )
        .append(
                category
        )
        .append(
                "\n"
        );


        int trustScore =
                trustService.calculateTrustScore(
                        email
                );

        String trustLevel =
                trustService.getTrustLevel(
                        email
                );

        email.setTrustScore(
                trustScore
        );


        brief.append(
                "   Trust: "
        )
        .append(
                trustLevel
        )
        .append(
                " ("
        )
        .append(
                trustScore
        )
        .append(
                "/100)"
        )
        .append(
                "\n"
        );


        String summary =
                summaryService.summarize(
                        email
                );

        email.setSummary(
                summary
        );


        brief.append(
                "   Summary: "
        )
        .append(
                safeText(
                        summary
                )
        )
        .append(
                "\n"
        );
    }


    // =====================================================
    // UTILITY
    // =====================================================

    private String safeText(
            String text) {

        if (text == null
                || text.isBlank()) {

            return "(No information)";
        }

        return text.trim();
    }
}