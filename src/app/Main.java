package app;

import model.Email;

import java.util.List;

/**
 * Temporary command-line entry point used for
 * testing the MailMate backend.
 *
 * The final JavaFX application will use MailMate
 * directly and will not depend on this class for
 * normal user interaction.
 */
public class Main {

    public static void main(String[] args) {

        MailMate mailMate =
                new MailMate();

        try {

            // =============================================
            // CONNECT
            // =============================================

            System.out.println(
                    "Connecting to Gmail..."
            );

            mailMate.connect();

            System.out.println(
                    "Connected successfully!"
            );

            System.out.println(
                    "Account: "
                            + mailMate.getAccountEmail()
            );


            // =============================================
            // LOAD EMAILS
            // =============================================

            List<Email> emails =
                    mailMate.loadEmails(10);

            System.out.println(
                    "\nEmails loaded: "
                            + emails.size()
            );


            // =============================================
            // BASIC METADATA
            // =============================================

            for (Email email : emails) {

                System.out.println(
                        "--------------------"
                );

                System.out.println(
                        "From: "
                                + email.getSender()
                );

                System.out.println(
                        "Subject: "
                                + email.getSubject()
                );

                System.out.println(
                        "Read: "
                                + email.isRead()
                );

                System.out.println(
                        "Starred: "
                                + email.isStarred()
                );

                System.out.println(
                        "Important: "
                                + email.isImportant()
                );

                System.out.println(
                        "Attachment: "
                                + email.hasAttachment()
                );
            }


            // =============================================
            // SEARCH TEST
            // =============================================

            System.out.println(
                    "\n===== SEARCH TEST ====="
            );

            List<Email> searchResults =
                    mailMate.search(
                            "important"
                    );

            System.out.println(
                    "Search results: "
                            + searchResults.size()
            );


            // =============================================
            // FILTER TEST
            // =============================================

            System.out.println(
                    "\n===== FILTER TEST ====="
            );

            System.out.println(
                    "Unread: "
                            + mailMate
                            .getUnreadEmails()
                            .size()
            );

            System.out.println(
                    "Starred: "
                            + mailMate
                            .getStarredEmails()
                            .size()
            );

            System.out.println(
                    "Important: "
                            + mailMate
                            .getImportantEmails()
                            .size()
            );

            System.out.println(
                    "Spam: "
                            + mailMate
                            .getSpamEmails()
                            .size()
            );

            System.out.println(
                    "With attachments: "
                            + mailMate
                            .getEmailsWithAttachments()
                            .size()
            );


            // =============================================
            // BRIEF TEST
            // =============================================

            System.out.println(
                    "\n===== BRIEF TEST ====="
            );

            String brief =
                    mailMate.generateBrief();

            System.out.println(
                    brief
            );


            // =============================================
            // BACKEND STATUS
            // =============================================

            System.out.println(
                    "\n===== BACKEND STATUS ====="
            );

            System.out.println(
                    "Gmail connection: OK"
            );

            System.out.println(
                    "Email retrieval: OK"
            );

            System.out.println(
                    "Search service: OK"
            );

            System.out.println(
                    "Filtering: OK"
            );

            System.out.println(
                    "Brief service: OK"
            );

            System.out.println(
                    "\nBackend test completed."
            );

        } catch (Exception e) {

            System.out.println(
                    "\nMailMate backend test failed."
            );

            System.out.println(
                    "Error: "
                            + e.getMessage()
            );

            e.printStackTrace();

        } finally {

            mailMate.disconnect();

            System.out.println(
                    "\nMailMate disconnected."
            );
        }
    }
}