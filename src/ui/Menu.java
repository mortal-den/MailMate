package ui;

import model.Email;
import service.BriefService;
import service.CategoryService;
import service.MailService;
import service.SearchService;
import service.SpamService;
import service.SummaryService;
import service.TrustService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Menu {

    private final Scanner scanner;

    private final MailService mailService;
    private final SearchService searchService;
    private final CategoryService categoryService;
    private final SpamService spamService;
    private final TrustService trustService;
    private final SummaryService summaryService;
    private final BriefService briefService;

    private List<Email> emails;

    public Menu(MailService mailService,
                List<Email> emails) {

        this.scanner = new Scanner(System.in);

        this.mailService = mailService;
        this.emails = emails;

        this.searchService = new SearchService();
        this.categoryService = new CategoryService();
        this.spamService = new SpamService();
        this.trustService = new TrustService();
        this.summaryService = new SummaryService();
        this.briefService = new BriefService();
    }

    /**
     * Starts the MailMate console menu.
     */
    public void start() {

        boolean running = true;

        while (running) {

            displayMenu();

            String choice =
                    scanner.nextLine().trim();

            switch (choice) {

                case "1":
                    showInbox();
                    break;

                case "2":
                    showUnreadEmails();
                    break;

                case "3":
                    showImportantEmails();
                    break;

                case "4":
                    showStarredEmails();
                    break;

                case "5":
                    searchEmails();
                    break;

                case "6":
                    showBrief();
                    break;

                case "7":
                    refreshEmails();
                    break;

                case "8":
                    running = false;
                    System.out.println(
                            "\nGoodbye!"
                    );
                    break;

                default:
                    System.out.println(
                            "\nInvalid option."
                    );
            }
        }
    }

    /**
     * Displays the main menu.
     */
    private void displayMenu() {

        System.out.println();
        System.out.println(
                "================================"
        );
        System.out.println(
                "            MAILMATE"
        );
        System.out.println(
                "================================"
        );

        System.out.println(
                "1. View inbox"
        );

        System.out.println(
                "2. View unread emails"
        );

        System.out.println(
                "3. View important emails"
        );

        System.out.println(
                "4. View starred emails"
        );

        System.out.println(
                "5. Search emails"
        );

        System.out.println(
                "6. View MailMate brief"
        );

        System.out.println(
                "7. Refresh inbox"
        );

        System.out.println(
                "8. Exit"
        );

        System.out.println(
                "================================"
        );

        System.out.print(
                "Choose an option: "
        );
    }

    /**
     * Displays the inbox.
     */
    private void showInbox() {

        if (emails.isEmpty()) {

            System.out.println(
                    "\nNo emails found."
            );

            return;
        }

        System.out.println(
                "\n========== INBOX =========="
        );

        displayEmailList(emails);
    }

    /**
     * Displays unread emails.
     */
    private void showUnreadEmails() {

        List<Email> unread =
                new ArrayList<>();

        for (Email email : emails) {

            if (!email.isRead()) {
                unread.add(email);
            }
        }

        System.out.println(
                "\n======= UNREAD EMAILS ======="
        );

        displayEmailList(unread);
    }

    /**
     * Displays important emails.
     */
    private void showImportantEmails() {

        List<Email> important =
                new ArrayList<>();

        for (Email email : emails) {

            if (email.isImportant()) {
                important.add(email);
            }
        }

        System.out.println(
                "\n====== IMPORTANT EMAILS ======"
        );

        displayEmailList(important);
    }

    /**
     * Displays starred emails.
     */
    private void showStarredEmails() {

        List<Email> starred =
                new ArrayList<>();

        for (Email email : emails) {

            if (email.isStarred()) {
                starred.add(email);
            }
        }

        System.out.println(
                "\n======= STARRED EMAILS ======="
        );

        displayEmailList(starred);
    }

    /**
     * Searches emails using SearchService.
     */
    private void searchEmails() {

        System.out.print(
                "\nEnter search keyword: "
        );

        String keyword =
                scanner.nextLine();

        List<Email> results =
                searchService.search(
                        emails,
                        keyword
                );

        System.out.println(
                "\n========= SEARCH RESULTS ========="
        );

        displayEmailList(results);
    }

    /**
     * Displays the MailMate brief.
     */
    private void showBrief() {

        String brief =
                briefService.generateBrief(
                        emails
                );

        System.out.println();
        System.out.println(brief);
    }

    /**
     * Retrieves fresh emails from Gmail.
     */
    private void refreshEmails() {

        try {

            System.out.println(
                    "\nRefreshing inbox..."
            );

            emails =
                    mailService.fetchEmails(20);

            analyzeEmails();

            System.out.println(
                    "Inbox refreshed successfully."
            );

        } catch (Exception e) {

            System.out.println(
                    "Unable to refresh inbox."
            );
        }
    }

    /**
     * Runs MailMate analysis services
     * against the currently loaded emails.
     */
    private void analyzeEmails() {

        for (Email email : emails) {

            CategoryService.Category category =
                    categoryService.categorize(
                            email
                    );

            int trustScore =
                    trustService.calculateTrustScore(
                            email
                    );

            boolean spam =
                    spamService.isSpam(
                            email
                    );

            String summary =
                    summaryService.summarize(
                            email
                    );

            email.setCategory(
                    category.toString()
            );

            email.setTrustScore(
                    trustScore
            );

            email.setSpam(
                    spam
            );

            email.setSummary(
                    summary
            );
        }
    }

    /**
     * Displays a list of emails without
     * exposing their complete body.
     */
    private void displayEmailList(
            List<Email> emailList) {

        if (emailList == null
                || emailList.isEmpty()) {

            System.out.println(
                    "No matching emails."
            );

            return;
        }

        int number = 1;

        for (Email email : emailList) {

            System.out.println();
            System.out.println(
                    number + ". "
                    + safeText(
                            email.getSubject()
                    )
            );

            System.out.println(
                    "   From: "
                    + safeText(
                            email.getSender()
                    )
            );

            System.out.println(
                    "   Date: "
                    + email.getReceivedDate()
            );

            System.out.println(
                    "   Category: "
                    + email.getCategory()
            );

            System.out.println(
                    "   Trust: "
                    + email.getTrustScore()
            );

            System.out.println(
                    "   Spam: "
                    + email.isSpam()
            );

            System.out.println(
                    "   Read: "
                    + email.isRead()
            );

            number++;
        }
    }

    /**
     * Prevents null values from appearing in the UI.
     */
    private String safeText(String text) {

        if (text == null
                || text.isBlank()) {

            return "(No information)";
        }

        return text.trim();
    }
}