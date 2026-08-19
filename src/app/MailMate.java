package app;

import model.Email;
import model.Settings;

import service.AttachmentService;
import service.BriefService;
import service.CategoryService;
import service.MailService;
import service.SearchService;
import service.SpamService;
import service.SummaryService;
import service.TrustService;

import java.io.File;
import java.io.IOException;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Main application/controller layer for MailMate.
 *
 * The JavaFX GUI communicates with this class instead
 * of communicating directly with individual services.
 *
 * Architecture:
 *
 * GUI
 *  ↓
 * MailMate
 *  ↓
 * Services
 *  ↓
 * Gmail / Local processing
 */
public class MailMate {

    // =====================================================
    // SERVICES
    // =====================================================

    private final MailService mailService;
    private final SearchService searchService;
    private final AttachmentService attachmentService;
    private final BriefService briefService;
    private final CategoryService categoryService;
    private final SpamService spamService;
    private final SummaryService summaryService;
    private final TrustService trustService;


    // =====================================================
    // SETTINGS
    // =====================================================

    /**
     * One shared Settings object for the entire
     * MailMate session.
     */
    private final Settings settings;


    // =====================================================
    // APPLICATION STATE
    // =====================================================

    private List<Email> emails;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public MailMate() {

        /*
         * Create shared settings first.
         */
        settings =
                new Settings();


        /*
         * Create independent services.
         */
        mailService =
                new MailService();

        searchService =
                new SearchService();

        attachmentService =
                new AttachmentService();

        categoryService =
                new CategoryService();

        summaryService =
                new SummaryService();

        trustService =
                new TrustService();


        /*
         * SpamService receives the shared Settings object.
         */
        spamService =
                new SpamService(
                        settings
                );


        /*
         * BriefService receives the SAME service
         * instances used by MailMate.
         *
         * This means:
         *
         * - same spam-sender rules
         * - same summary logic
         * - same category logic
         * - same trust logic
         */
        briefService =
                new BriefService(
                        summaryService,
                        categoryService,
                        trustService,
                        spamService
                );


        emails =
                new ArrayList<>();
    }


    // =====================================================
    // APPLICATION STARTUP
    // =====================================================

    /**
     * Starts the MailMate backend.
     *
     * Kept for compatibility with MailMateApp.java.
     */
    public void startApplication()
            throws Exception {

        long startupStart = System.nanoTime();

        System.out.println();
        System.out.println("===== MAILMATE STARTUP TIMING =====");

        long connectStart = System.nanoTime();

        System.out.println("[STARTUP] Connecting to Gmail...");
        connect();

        long connectTime = System.nanoTime() - connectStart;

        System.out.printf(
                "[STARTUP] Gmail connection: %.2f seconds%n",
                connectTime / 1_000_000_000.0
        );

        long totalTime = System.nanoTime() - startupStart;

        System.out.printf(
                "[STARTUP] Backend startup total: %.2f seconds%n",
                totalTime / 1_000_000_000.0
        );

        System.out.println(
                "=================================="
        );
    }


    // =====================================================
    // CONNECTION
    // =====================================================

    /**
     * Connects MailMate to Gmail using OAuth.
     */
    public void connect()
            throws Exception {

        mailService.connect();
    }


    public boolean isConnected() {

        return mailService.isConnected();
    }


    public String getAccountEmail() {

        return mailService.getAccountEmail();
    }


    public void disconnect() {

        mailService.disconnect();
    }


    /**
     * Switches MailMate to a different Google account.
     *
     * The MailService clears the current OAuth credential and
     * starts a fresh authorization flow. All emails currently
     * held by this MailMate session are cleared before the
     * new account is connected.
     */
    public void changeAccount()
            throws Exception {

        /*
         * Clear the application-side email state first so
         * messages from the previous account cannot remain
         * visible while the new account is being authorized.
         */
        emails.clear();

        mailService.changeAccount();
    }


    // =====================================================
    // SETTINGS
    // =====================================================

    /**
     * Returns the shared application settings.
     */
    public Settings getSettings() {

        return settings;
    }


    // =====================================================
    // EMAIL RETRIEVAL + PROCESSING
    // =====================================================

    /**
     * Loads emails from Gmail and runs the complete
     * local MailMate analysis pipeline.
     *
     * Pipeline:
     *
     * Gmail
     *   ↓
     * Email
     *   ↓
     * Spam
     *   ↓
     * Promotional
     *   ↓
     * Category
     *   ↓
     * Trust
     *   ↓
     * Summary
     */
    public List<Email> loadEmails(
            int maxResults)
            throws IOException {

        long loadStart = System.nanoTime();

        System.out.println();
        System.out.println("===== EMAIL LOAD TIMING =====");

        long fetchStart = System.nanoTime();

        System.out.println(
                "[LOAD] Fetching up to "
                        + maxResults
                        + " emails from Gmail..."
        );

        emails =
                new ArrayList<>(
                        mailService.fetchEmails(
                                maxResults
                        )
                );

        long fetchTime = System.nanoTime() - fetchStart;

        System.out.printf(
                "[LOAD] Gmail email retrieval: %.2f seconds%n",
                fetchTime / 1_000_000_000.0
        );

        long analysisStart = System.nanoTime();

        System.out.println(
                "[LOAD] Running local email analysis..."
        );

        analyzeEmails();

        long analysisTime =
                System.nanoTime() - analysisStart;

        System.out.printf(
                "[LOAD] Local analysis: %.2f seconds%n",
                analysisTime / 1_000_000_000.0
        );

        long totalTime =
                System.nanoTime() - loadStart;

        System.out.printf(
                "[LOAD] Email loading total: %.2f seconds%n",
                totalTime / 1_000_000_000.0
        );

        System.out.println(
                "============================="
        );

        return getEmails();
    }


    /**
     * Runs all local analysis services on
     * currently loaded emails.
     */
    private void analyzeEmails() {

        for (Email email : emails) {

            if (email == null) {
                continue;
            }


            // =============================================
            // 1. SPAM
            // =============================================

            boolean spam =
                    spamService.isSpam(
                            email
                    );

            email.setSpam(
                    spam
            );


            // =============================================
            // 2. PROMOTIONAL
            // =============================================

            boolean promotional =
                    spamService.isPromotional(
                            email
                    );

            /*
             * Spam takes precedence over promotion.
             */
            if (spam) {

                promotional = false;
            }

            email.setPromotional(
                    promotional
            );


            // =============================================
            // 3. CATEGORY
            // =============================================

            CategoryService.Category category =
                    categoryService.categorize(
                            email
                    );

            email.setCategory(
                    category.name()
            );


            // =============================================
            // 4. TRUST
            // =============================================

            int trustScore =
                    trustService.calculateTrustScore(
                            email
                    );

            email.setTrustScore(
                    trustScore
            );


            // =============================================
            // 5. SUMMARY
            // =============================================

            if (settings.isSummaryEnabled()) {

                String summary =
                        summaryService.summarize(
                                email
                        );

                email.setSummary(
                        summary
                );

            } else {

                email.setSummary(
                        ""
                );
            }
        }
    }


    /**
     * Returns a copy of currently loaded emails.
     */
    public List<Email> getEmails() {

        return new ArrayList<>(
                emails
        );
    }


    /**
     * Returns currently loaded emails with attachments.
     */
    public List<Email> getEmailsWithAttachments() {

        List<Email> results =
                new ArrayList<>();

        for (Email email : emails) {

            if (email != null
                    && email.hasAttachment()) {

                results.add(
                        email
                );
            }
        }

        return results;
    }


    // =====================================================
    // SEARCH
    // =====================================================

    public List<Email> search(
            String keyword) {

        return searchService.search(
                emails,
                keyword
        );
    }


    public List<Email> searchBySender(
            String sender) {

        return searchService.searchBySender(
                emails,
                sender
        );
    }


    public List<Email> searchBySubject(
            String subject) {

        return searchService.searchBySubject(
                emails,
                subject
        );
    }


    public List<Email> searchByDateRange(
            LocalDateTime start,
            LocalDateTime end) {

        return searchService.searchByDateRange(
                emails,
                start,
                end
        );
    }


    // =====================================================
    // STANDARD FILTERS
    // =====================================================

    public List<Email> getUnreadEmails() {

        return searchService.searchUnread(
                emails
        );
    }


    public List<Email> getStarredEmails() {

        return searchService.searchStarred(
                emails
        );
    }


    public List<Email> getImportantEmails() {

        return searchService.searchImportant(
                emails
        );
    }


    // =====================================================
    // SPAM
    // =====================================================

    /**
     * Returns all emails currently classified as spam.
     */
    public List<Email> getSpamEmails() {

        List<Email> spamEmails =
                spamService.findSpam(
                        emails
                );

        for (Email email : emails) {

            if (email == null) {
                continue;
            }

            boolean spam =
                    spamEmails.contains(
                            email
                    );

            email.setSpam(
                    spam
            );

            if (spam) {

                email.setPromotional(
                        false
                );
            }
        }

        return spamEmails;
    }


    public int getSpamCount() {

        return getSpamEmails().size();
    }


    public int calculateSpamScore(
            Email email) {

        return spamService.calculateSpamScore(
                email
        );
    }


    public boolean isSpam(
            Email email) {

        return spamService.isSpam(
                email
        );
    }


    // =====================================================
    // MANUAL MARK AS SPAM
    // =====================================================

    /**
     * Marks the sender of an email as a spam source.
     *
     * The current email is immediately marked as spam.
     * Future emails from this sender are classified as
     * spam during the current MailMate session.
     */
    public void markAsSpam(
            Email email) {

        if (email == null) {
            return;
        }

        spamService.markAsSpam(
                email
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

        spamService.unmarkSenderAsSpam(
                sender
        );

        String normalizedSender =
                spamService.extractEmailAddress(
                        sender
                );

        for (Email email : emails) {

            if (email == null) {
                continue;
            }

            String emailSender =
                    spamService.extractEmailAddress(
                            email.getSender()
                    );

            if (emailSender.equalsIgnoreCase(
                    normalizedSender
            )) {

                boolean spam =
                        spamService.isSpam(
                                email
                        );

                email.setSpam(
                        spam
                );

                email.setPromotional(
                        spam
                                ? false
                                : spamService.isPromotional(
                                        email
                                )
                );
            }
        }
    }


    public boolean isSenderMarkedAsSpam(
            Email email) {

        if (email == null) {
            return false;
        }

        return spamService.isSenderMarkedAsSpam(
                email.getSender()
        );
    }


    public Set<String>
    getManuallyBlockedSenders() {

        return spamService
                .getManuallyBlockedSenders();
    }


    public void clearManuallyBlockedSenders() {

        spamService.clearManuallyBlockedSenders();

        analyzeEmails();
    }


    // =====================================================
    // DELETE ALL SPAM
    // =====================================================

    /**
     * Moves all currently detected spam emails
     * to Gmail Trash.
     *
     * The messages are NOT permanently deleted.
     */
    public void deleteAllSpam()
            throws IOException {

        List<Email> spamEmails =
                new ArrayList<>(
                        getSpamEmails()
                );

        if (spamEmails.isEmpty()) {

            return;
        }

        mailService.moveEmailsToTrash(
                spamEmails
        );

        emails.removeAll(
                spamEmails
        );
    }


    // =====================================================
    // PROMOTIONAL
    // =====================================================

    public List<Email> getPromotionalEmails() {

        List<Email> promotionalEmails =
                spamService.findPromotional(
                        emails
                );

        for (Email email : emails) {

            if (email == null) {
                continue;
            }

            boolean promotional =
                    promotionalEmails.contains(
                            email
                    );

            email.setPromotional(
                    promotional
            );

            /*
             * Spam must never remain promotional.
             */
            if (email.isSpam()) {

                email.setPromotional(
                        false
                );
            }
        }

        return promotionalEmails;
    }


    public int getPromotionalCount() {

        return getPromotionalEmails().size();
    }


    /**
     * Moves all currently classified promotional emails
     * to Gmail Trash and removes them from the current
     * MailMate session.
     *
     * Spam emails are not included because the MailMate
     * analysis pipeline gives spam precedence over promotion.
     *
     * @return number of promotional emails moved to Trash
     * @throws IOException if Gmail cannot complete the operation
     */
    public int deleteAllPromotionalEmails()
            throws IOException {

        List<Email> promotionalEmails =
                new ArrayList<>(
                        getPromotionalEmails()
                );

        if (promotionalEmails.isEmpty()) {
            return 0;
        }

        mailService.movePromotionalEmailsToTrash(
                promotionalEmails
        );

        emails.removeAll(
                promotionalEmails
        );

        return promotionalEmails.size();
    }


    public boolean isPromotional(
            Email email) {

        return spamService.isPromotional(
                email
        );
    }


    public List<Email> getNonSpamEmails() {

        return spamService.findNonSpam(
                emails
        );
    }


    // =====================================================
    // CATEGORY
    // =====================================================

    public CategoryService.Category categorize(
            Email email) {

        CategoryService.Category category =
                categoryService.categorize(
                        email
                );

        if (email != null) {

            email.setCategory(
                    category.name()
            );
        }

        return category;
    }


    public List<Email> getEmailsByCategory(
            CategoryService.Category category) {

        return categoryService.findByCategory(
                emails,
                category
        );
    }


    public String getCategory(
            Email email) {

        if (email == null) {

            return "OTHER";
        }

        return email.getCategory();
    }


    // =====================================================
    // TRUST
    // =====================================================

    public int calculateTrustScore(
            Email email) {

        int score =
                trustService.calculateTrustScore(
                        email
                );

        if (email != null) {

            email.setTrustScore(
                    score
            );
        }

        return score;
    }


    public boolean isTrusted(
            Email email) {

        return trustService.isTrusted(
                email
        );
    }


    public String getTrustLevel(
            Email email) {

        return trustService.getTrustLevel(
                email
        );
    }


    public String getTrustDescription(
            Email email) {

        return trustService.getTrustDescription(
                email
        );
    }


    // =====================================================
    // SUMMARY
    // =====================================================

    public String summarize(
            Email email) {

        String summary =
                summaryService.summarize(
                        email
                );

        if (email != null) {

            email.setSummary(
                    summary
            );
        }

        return summary;
    }


    public String summarizeWithSubject(
            Email email) {

        String summary =
                summaryService
                        .summarizeWithSubject(
                                email
                        );

        if (email != null) {

            email.setSummary(
                    summary
            );
        }

        return summary;
    }


    // =====================================================
    // EMAIL ACTIONS
    // =====================================================

    public void markAsRead(
            Email email)
            throws IOException {

        mailService.markAsRead(
                email
        );
    }


    public void markAsUnread(
            Email email)
            throws IOException {

        mailService.markAsUnread(
                email
        );
    }


    public void star(
            Email email)
            throws IOException {

        mailService.star(
                email
        );
    }


    public void unstar(
            Email email)
            throws IOException {

        mailService.unstar(
                email
        );
    }


    public void markImportant(
            Email email)
            throws IOException {

        mailService.markImportant(
                email
        );
    }


    public void markNotImportant(
            Email email)
            throws IOException {

        mailService.markNotImportant(
                email
        );
    }


    /**
     * Archives one email.
     */
    public void archive(
            Email email)
            throws IOException {

        mailService.archive(
                email
        );

        emails.remove(
                email
        );
    }


    /**
     * Moves one email to Gmail Trash.
     */
    public void moveToTrash(
            Email email)
            throws IOException {

        mailService.moveToTrash(
                email
        );

        emails.remove(
                email
        );
    }


    // =====================================================
    // ATTACHMENTS
    // =====================================================

    public List<File> downloadAttachments(
            Email email)
            throws IOException {

        return mailService.downloadAttachments(
                email
        );
    }


    public List<File> getAttachmentFiles(
            Email email)
            throws IOException {

        return mailService.getAttachmentFiles(
                email
        );
    }


    public File downloadAttachment(
            Email email,
            String fileName)
            throws IOException {

        return mailService.downloadAttachment(
                email,
                fileName
        );
    }


    public String extractAttachmentText(
            File file)
            throws IOException {

        return attachmentService.extractText(
                file
        );
    }


    public List<String> extractEmailAttachments(
            Email email)
            throws IOException {

        List<File> files =
                downloadAttachments(
                        email
                );

        return attachmentService.extractText(
                files
        );
    }


    public boolean isAttachmentSupported(
            File file) {

        return attachmentService.isSupported(
                file
        );
    }


    public String getAttachmentType(
            File file) {

        return attachmentService.getAttachmentType(
                file
        );
    }


    public String getAttachmentSize(
            File file)
            throws IOException {

        return attachmentService
                .getFormattedFileSize(
                        file
                );
    }


    // =====================================================
    // BRIEF
    // =====================================================

    /**
     * Generates the complete MailMate brief.
     */
    public String generateBrief() {

        return briefService.generateBrief(
                emails
        );
    }


    /**
     * Generates a brief containing only unread emails.
     */
    public String generateUnreadBrief() {

        return briefService.generateUnreadBrief(
                emails
        );
    }
}