package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an email account connected to MailMate.
 *
 * An account can contain multiple folders, such as Inbox,
 * Spam, and other user-created folders.
 */
public class Account {

    // ===== Account Information =====
    private final String emailAddress;
    private final String provider;

    // ===== Account Folders =====
    private final List<Folder> folders;

    /**
     * Constructs a new Account.
     *
     * @param emailAddress Email address of the account
     * @param provider     Email service provider
     */
    public Account(String emailAddress, String provider) {
        this.emailAddress = emailAddress;
        this.provider = provider;
        this.folders = new ArrayList<>();
    }

    // =====================================================
    // Getters
    // =====================================================

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getProvider() {
        return provider;
    }

    public List<Folder> getFolders() {
        return folders;
    }

    // =====================================================
    // Folder Management
    // =====================================================

    /**
     * Adds a folder to this account.
     *
     * @param folder Folder to add
     */
    public void addFolder(Folder folder) {
        if (folder != null && !folders.contains(folder)) {
            folders.add(folder);
        }
    }

    /**
     * Removes a folder from this account.
     *
     * @param folder Folder to remove
     */
    public void removeFolder(Folder folder) {
        folders.remove(folder);
    }

    /**
     * Checks whether this account contains a particular folder.
     *
     * @param folder Folder to check
     * @return true if the folder exists in this account
     */
    public boolean containsFolder(Folder folder) {
        return folders.contains(folder);
    }

    /**
     * Returns the number of folders in this account.
     *
     * @return Number of folders
     */
    public int getFolderCount() {
        return folders.size();
    }

    /**
     * Returns a readable representation of the account.
     *
     * @return Account information
     */
    @Override
    public String toString() {
        return "Account {" +
                "\n  Email Address : " + emailAddress +
                "\n  Provider      : " + provider +
                "\n  Folder Count  : " + folders.size() +
                "\n}";
    }
}
