package model;

import java.util.ArrayList;
import java.util.List;

public class Folder {

    // ===== Folder Information =====
    private final String name;
    private final List<Email> emails;

    /**
     * Constructs a new Folder.
     *
     * @param name Name of the folder
     */
    public Folder(String name) {
        this.name = name;
        this.emails = new ArrayList<>();
    }

    // =====================================================
    // Getters
    // =====================================================

    public String getName() {
        return name;
    }

    public List<Email> getEmails() {
        return emails;
    }

    // =====================================================
    // Email Management
    // =====================================================

    /**
     * Adds an email to this folder.
     *
     * @param email Email to add
     */
    public void addEmail(Email email) {
        if (email != null && !emails.contains(email)) {
            emails.add(email);
        }
    }

    /**
     * Removes an email from this folder.
     *
     * @param email Email to remove
     */
    public void removeEmail(Email email) {
        emails.remove(email);
    }

    /**
     * Checks whether this folder contains a particular email.
     *
     * @param email Email to check
     * @return true if the email exists in this folder
     */
    public boolean containsEmail(Email email) {
        return emails.contains(email);
    }

    /**
     * Returns the number of emails currently in the folder.
     *
     * @return Number of emails
     */
    public int getEmailCount() {
        return emails.size();
    }

    /**
     * Checks whether the folder contains no emails.
     *
     * @return true if the folder is empty
     */
    public boolean isEmpty() {
        return emails.isEmpty();
    }

    /**
     * Removes all emails from the folder.
     */
    public void clear() {
        emails.clear();
    }

    /**
     * Returns a readable representation of the folder.
     *
     * @return Folder information
     */
    @Override
    public String toString() {
        return "Folder {" +
                "\n  Name        : " + name +
                "\n  Email Count : " + emails.size() +
                "\n}";
    }
}
