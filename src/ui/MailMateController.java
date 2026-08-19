package ui;

import app.MailMate;
import model.Email;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.concurrent.Task;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * JavaFX controller for MailMate.
 *
 * Handles:
 *
 * - Gmail startup
 * - Inbox navigation
 * - Search
 * - Email selection
 * - Summary
 * - Spam
 * - Promotions
 * - Archive
 * - Delete
 * - Attachments
 * - Attachment download
 * - Attachment text extraction
 * - Inbox organization
 */
public class MailMateController {

    // =====================================================
    // BACKEND
    // =====================================================

    private MailMate mailMate;

    /*
     * Prevents multiple refresh/load operations from running at once.
     * Gmail/network work must not run on the JavaFX Application Thread.
     */
    private final AtomicBoolean loadingEmails = new AtomicBoolean(false);


    // =====================================================
    // DISPLAYED EMAILS
    // =====================================================

    private final ObservableList<Email> displayedEmails =
            FXCollections.observableArrayList();


    // =====================================================
    // SELECTED ATTACHMENTS
    // =====================================================

    private final ObservableList<File> selectedAttachments =
            FXCollections.observableArrayList();


    // =====================================================
    // DATE FORMAT
    // =====================================================

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, HH:mm"
            );


    // =====================================================
    // CURRENT VIEW
    // =====================================================

    private ViewMode currentView =
            ViewMode.INBOX;


    private enum ViewMode {

        INBOX,
        UNREAD,
        STARRED,
        IMPORTANT,
        SPAM,
        PROMOTIONAL,
        SEARCH
    }


    // =====================================================
    // FXML CONTROLS
    // =====================================================

    @FXML
    private ListView<Email> emailListView;

    @FXML
    private TextField searchField;

    @FXML
    private Label pageTitle;

    @FXML
    private Label statusLabel;

    @FXML
    private Label accountLabel;

    @FXML
    private Button changeAccountButton;

    @FXML
    private Label emailCountLabel;

    @FXML
    private Label unreadCountLabel;

    @FXML
    private Label spamCountLabel;

    @FXML
    private Label promotionalCountLabel;

    @FXML
    private Label senderLabel;

    @FXML
    private Label subjectLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label trustScoreLabel;

    @FXML
    private Label attachmentLabel;

    @FXML
    private TextArea summaryArea;

    @FXML
    private TextArea bodyArea;

    @FXML
    private ListView<File> attachmentListView;

    @FXML
    private Button summaryButton;

    @FXML
    private Button starButton;

    @FXML
    private Button markSpamButton;

    @FXML
    private Button archiveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button deleteSpamButton;

    @FXML
    private Button deletePromotionalButton;

    @FXML
    private Button downloadAttachmentButton;

    @FXML
    private Button extractAttachmentButton;


    // =====================================================
    // INITIALIZATION
    // =====================================================

    @FXML
    public void initialize() {

        /*
         * Connect the observable lists to the JavaFX controls.
         */
        emailListView.setItems(
                displayedEmails
        );

        attachmentListView.setItems(
                selectedAttachments
        );


        /*
         * Configure the email list.
         */
        configureEmailList();


        /*
         * Configure the attachment list.
         */
        configureAttachmentList();


        /*
         * Detect selected email changes.
         */
        emailListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldEmail, newEmail) -> {

                            if (newEmail == null) {

                                clearEmailDetails();

                            } else {

                                displayEmail(
                                        newEmail
                                );
                            }
                        }
                );


        /*
         * Detect selected attachment changes.
         */
        attachmentListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldFile, newFile) -> {

                            updateAttachmentButtons(
                                    newFile
                            );
                        }
                );


        clearEmailDetails();

        updateCounters();

        updatePromotionalBulkButton(false);
    }


    // =====================================================
    // APPLICATION STARTUP
    // =====================================================

    /**
     * Starts MailMate and connects to Gmail.
     *
     * This method is called by MailMateApp.
     */
    public void startApplication() {

        if (mailMate == null) {
            mailMate = new MailMate();
        }

        setLoadingState(true, "Connecting to Gmail...");

        Task<Void> task = new Task<>() {

            @Override
            protected Void call() throws Exception {
                mailMate.startApplication();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setLoadingState(false, null);
            updateAccountInformation();
            loadInbox();
        });

        task.setOnFailed(event -> {
            setLoadingState(false, null);

            Throwable error = task.getException();

            statusLabel.setText("Gmail connection failed");

            showError(
                    "Gmail Connection Failed",
                    getErrorMessage(
                            error instanceof Exception
                                    ? (Exception) error
                                    : new Exception(error)
                    )
            );
        });

        Thread thread = new Thread(task, "MailMate-Gmail-Startup");
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * Allows MailMateApp or another caller
     * to provide an existing MailMate instance.
     */
    public void setMailMate(
            MailMate mailMate) {

        this.mailMate =
                mailMate;

        updateAccountInformation();


        if (mailMate != null
                && mailMate.isConnected()) {

            loadInbox();
        }
    }


    // =====================================================
    // CHANGE ACCOUNT
    // =====================================================

    /**
     * Disconnects the current Google account, clears the
     * current MailMate state and starts a fresh OAuth flow.
     *
     * The operation runs on a background thread because
     * OAuth and Gmail connection work may involve network I/O.
     */
    @FXML
    private void handleChangeAccount() {

        if (mailMate == null) {

            showInformation(
                    "MailMate Not Ready",
                    "MailMate has not been initialized yet."
            );

            return;
        }

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Change Account"
        );

        confirmation.setHeaderText(
                "Switch the Gmail account?"
        );

        confirmation.setContentText(
                "The current account will be disconnected and "
                        + "MailMate will ask you to authorize a "
                        + "Google account again."
        );

        confirmation
                .getButtonTypes()
                .setAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );

        confirmation
                .showAndWait()
                .ifPresent(result -> {

                    if (result != ButtonType.OK) {
                        return;
                    }

                    if (!loadingEmails.compareAndSet(
                            false,
                            true
                    )) {

                        showInformation(
                                "Operation In Progress",
                                "Please wait for the current Gmail operation to finish."
                        );

                        return;
                    }

                    currentView =
                            ViewMode.INBOX;

                    displayedEmails.clear();

                    clearEmailDetails();

                    updateCounters();

                    setLoadingState(
                            true,
                            "Changing Google account..."
                    );

                    if (changeAccountButton != null) {
                        changeAccountButton.setDisable(true);
                    }

                    accountLabel.setText(
                            "Connecting..."
                    );

                    Task<Void> task =
                            new Task<>() {

                                @Override
                                protected Void call()
                                        throws Exception {

                                    mailMate.changeAccount();

                                    return null;
                                }
                            };

                    task.setOnSucceeded(event -> {

                        loadingEmails.set(false);

                        updateAccountInformation();

                        /*
                         * The new account is connected. Reuse the
                         * existing inbox loader so the same tested
                         * Gmail loading path is used.
                         */
                        if (changeAccountButton != null) {
                            changeAccountButton.setDisable(false);
                        }

                        setLoadingState(
                                false,
                                "Account changed successfully"
                        );

                        loadInbox();
                    });

                    task.setOnFailed(event -> {

                        loadingEmails.set(false);

                        /*
                         * MailMate.changeAccount() disconnects the
                         * old account before starting fresh OAuth.
                         * Therefore, on failure we must not pretend
                         * that the old account is still connected.
                         */
                        displayedEmails.clear();

                        clearEmailDetails();

                        updateAccountInformation();

                        if (changeAccountButton != null) {
                            changeAccountButton.setDisable(false);
                        }

                        setLoadingState(
                                false,
                                "Account change failed"
                        );

                        Throwable error =
                                task.getException();

                        showError(
                                "Unable to Change Account",
                                getErrorMessage(
                                        error instanceof Exception
                                                ? (Exception) error
                                                : new Exception(error)
                                )
                        );
                    });

                    Thread thread =
                            new Thread(
                                    task,
                                    "MailMate-Change-Account"
                            );

                    thread.setDaemon(true);
                    thread.start();
                });
    }


    // =====================================================
    // EMAIL LIST
    // =====================================================

    /**
     * Configures how emails appear in the inbox.
     */
    private void configureEmailList() {

        emailListView.setCellFactory(
                listView ->
                        new ListCell<>() {

                            private final Label indicatorLabel =
                                    new Label();

                            private final Label subjectLabel =
                                    new Label();

                            private final Label senderLabel =
                                    new Label();

                            private final Label metadataLabel =
                                    new Label();

                            private final Label dateLabel =
                                    new Label();

                            private final VBox textBox =
                                    new VBox(3);

                            private final HBox root =
                                    new HBox(10);

                            {
                                subjectLabel.getStyleClass()
                                        .add("inbox-subject");

                                senderLabel.getStyleClass()
                                        .add("inbox-sender");

                                metadataLabel.getStyleClass()
                                        .add("inbox-metadata");

                                dateLabel.getStyleClass()
                                        .add("inbox-date");

                                indicatorLabel.getStyleClass()
                                        .add("inbox-indicators");

                                textBox.getChildren().addAll(
                                        subjectLabel,
                                        senderLabel,
                                        metadataLabel
                                );

                                HBox.setHgrow(
                                        textBox,
                                        Priority.ALWAYS
                                );

                                root.getChildren().addAll(
                                        indicatorLabel,
                                        textBox,
                                        dateLabel
                                );

                                setGraphic(root);
                            }

                            @Override
                            protected void updateItem(
                                    Email email,
                                    boolean empty) {

                                super.updateItem(
                                        email,
                                        empty
                                );

                                if (empty
                                        || email == null) {

                                    setGraphic(null);
                                    setText(null);

                                    return;
                                }

                                /*
                                 * Compact visual priority indicators.
                                 * Their order mirrors the inbox priority:
                                 * unread, important, starred.
                                 */
                                StringBuilder indicators =
                                        new StringBuilder();

                                if (!email.isRead()) {
                                    indicators.append("● ");
                                }

                                if (email.isImportant()) {
                                    indicators.append("! ");
                                }

                                if (email.isStarred()) {
                                    indicators.append("★");
                                }

                                indicatorLabel.setText(
                                        indicators.toString()
                                );

                                /*
                                 * Main message information.
                                 */
                                subjectLabel.setText(
                                        safe(email.getSubject())
                                );

                                senderLabel.setText(
                                        safe(email.getSender())
                                );

                                /*
                                 * Secondary metadata stays on one
                                 * dedicated line instead of mixing it
                                 * into the subject/sender text.
                                 */
                                StringBuilder metadata =
                                        new StringBuilder();

                                if (email.isSpam()) {

                                    metadata.append("SPAM");

                                } else if (email.isPromotional()) {

                                    metadata.append("PROMOTION");

                                } else {

                                    String category =
                                            safe(email.getCategory());

                                    if (!category.isBlank()
                                            && !category.equals(
                                                    "(No information)"
                                            )) {

                                        metadata.append(
                                                category
                                        );
                                    }
                                }

                                if (email.hasAttachment()) {

                                    appendMetadata(
                                            metadata,
                                            "📎 Attachment"
                                    );
                                }

                                if (email.getTrustScore() > 0) {

                                    appendMetadata(
                                            metadata,
                                            "Trust "
                                                    + email.getTrustScore()
                                                    + "/100"
                                    );
                                }

                                metadataLabel.setText(
                                        metadata.toString()
                                );

                                /*
                                 * Keep the received date visible so the
                                 * chronological ordering is obvious.
                                 */
                                if (email.getReceivedDate() != null) {

                                    dateLabel.setText(
                                            email.getReceivedDate()
                                                    .format(
                                                            DATE_FORMATTER
                                                    )
                                    );

                                } else {

                                    dateLabel.setText("");
                                }

                                setGraphic(root);
                                setText(null);
                            }
                        }
        );
    }

    /**
     * Adds metadata to the current line with a separator.
     */
    private void appendMetadata(
            StringBuilder metadata,
            String value) {

        if (value == null
                || value.isBlank()) {

            return;
        }

        if (metadata.length() > 0) {

            metadata.append("  •  ");
        }

        metadata.append(value);
    }


    // =====================================================
    // ATTACHMENT LIST
    // =====================================================

    /**
     * Configures how attachments appear in the
     * attachment ListView.
     *
     * IMPORTANT:
     * There is only ONE configureAttachmentList()
     * method in this controller.
     */
    private void configureAttachmentList() {

        attachmentListView.setCellFactory(
                listView ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    File file,
                                    boolean empty) {

                                super.updateItem(
                                        file,
                                        empty
                                );


                                if (empty
                                        || file == null) {

                                    setText(null);

                                    return;
                                }


                                String type =
                                        "UNKNOWN";

                                String size =
                                        "Unknown size";


                                if (mailMate != null) {

                                    type =
                                            mailMate
                                                    .getAttachmentType(
                                                            file
                                                    );


                                    try {

                                        size =
                                                mailMate
                                                        .getAttachmentSize(
                                                                file
                                                        );

                                    } catch (IOException ignored) {
                                        // Keep fallback text.
                                    }
                                }


                                setText(
                                        "📎 "
                                                + file.getName()
                                                + "\n"
                                                + type
                                                + " • "
                                                + size
                                );
                            }
                        }
        );
    }


    // =====================================================
    // REFRESH
    // =====================================================

    @FXML
    private void handleRefresh() {

        loadInbox();
    }


    // =====================================================
    // LOAD INBOX
    // =====================================================

    private void loadInbox() {

        if (mailMate == null) {
            statusLabel.setText("MailMate is not initialized");
            return;
        }

        /*
         * Do not allow a second Gmail request while one is already running.
         */
        if (!loadingEmails.compareAndSet(false, true)) {
            statusLabel.setText("Refresh already in progress...");
            return;
        }

        currentView = ViewMode.INBOX;
        pageTitle.setText("Inbox");
        setLoadingState(true, "Loading emails...");

        Task<List<Email>> task = new Task<>() {

            @Override
            protected List<Email> call() throws Exception {
                /*
                 * This is the important part:
                 * loadEmails() performs Gmail/network work, so it runs
                 * on this background thread instead of the JavaFX thread.
                 */
                return mailMate.loadEmails(100);
            }
        };

        task.setOnSucceeded(event -> {

            try {
                List<Email> emails = task.getValue();

                displayedEmails.setAll(
                        organizeInbox(emails)
                );

                updateCounters();

                statusLabel.setText(
                        emails.size() + " emails loaded"
                );

                disableBulkSpamButton();
                selectFirstResult();

            } finally {
                loadingEmails.set(false);
                setLoadingState(false, null);
            }
        });

        task.setOnFailed(event -> {

            loadingEmails.set(false);
            setLoadingState(false, null);

            statusLabel.setText("Unable to load emails");

            Throwable error = task.getException();

            showError(
                    "Unable to Load Emails",
                    getErrorMessage(
                            error instanceof Exception
                                    ? (Exception) error
                                    : new Exception(error)
                    )
            );
        });

        Thread thread = new Thread(task, "MailMate-Email-Loader");
        thread.setDaemon(true);
        thread.start();
    }


    // =====================================================
    // INBOX ORGANIZATION
    // =====================================================

    /**
     * Organizes the inbox so more relevant emails
     * appear before ordinary messages.
     *
     * Priority:
     *
     * Unread
     * Important
     * Starred
     * Attachment
     *
     * Spam/promotions are intentionally lower.
     */
    private List<Email> organizeInbox(
            List<Email> emails) {

        List<Email> organized =
                new ArrayList<>();


        if (emails == null) {

            return organized;
        }


        for (Email email :
                emails) {

            if (email != null) {

                organized.add(
                        email
                );
            }
        }


        organized.sort(
                Comparator
                        .comparingInt(
                                this::getInboxPriority
                        )
                        .reversed()
                        .thenComparing(
                                Email::getReceivedDate,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
        );


        return organized;
    }


    /**
     * Calculates inbox priority.
     */
    private int getInboxPriority(
            Email email) {

        int priority = 0;


        if (!email.isRead()) {

            priority += 4;
        }


        if (email.isImportant()) {

            priority += 3;
        }


        if (email.isStarred()) {

            priority += 2;
        }


        if (email.hasAttachment()) {

            priority += 1;
        }


        /*
         * Spam should not dominate the inbox.
         */
        if (email.isSpam()) {

            priority -= 10;
        }


        /*
         * Promotional messages are lower priority,
         * but not as strongly suppressed as spam.
         */
        if (email.isPromotional()) {

            priority -= 2;
        }


        return priority;
    }


    // =====================================================
    // INBOX VIEW
    // =====================================================

    @FXML
    private void showInbox() {

        if (mailMate == null) {

            return;
        }


        currentView =
                ViewMode.INBOX;


        pageTitle.setText(
                "Inbox"
        );


        displayedEmails.setAll(
                organizeInbox(
                        mailMate.getEmails()
                )
        );


        statusLabel.setText(
                displayedEmails.size()
                        + " emails"
        );


        disableBulkSpamButton();


        selectFirstResult();
    }


    // =====================================================
    // UNREAD VIEW
    // =====================================================

    @FXML
    private void showUnread() {

        if (mailMate == null) {

            return;
        }


        currentView =
                ViewMode.UNREAD;


        List<Email> results =
                mailMate.getUnreadEmails();


        displayedEmails.setAll(
                organizeInbox(
                        results
                )
        );


        pageTitle.setText(
                "Unread"
        );


        statusLabel.setText(
                results.size()
                        + " unread emails"
        );


        disableBulkSpamButton();


        selectFirstResult();
    }


    // =====================================================
    // STARRED VIEW
    // =====================================================

    @FXML
    private void showStarred() {

        if (mailMate == null) {

            return;
        }


        currentView =
                ViewMode.STARRED;


        List<Email> results =
                mailMate.getStarredEmails();


        displayedEmails.setAll(
                organizeInbox(
                        results
                )
        );


        pageTitle.setText(
                "Starred"
        );


        statusLabel.setText(
                results.size()
                        + " starred emails"
        );


        disableBulkSpamButton();


        selectFirstResult();
    }


    // =====================================================
    // IMPORTANT VIEW
    // =====================================================

    @FXML
    private void showImportant() {

        if (mailMate == null) {

            return;
        }


        currentView =
                ViewMode.IMPORTANT;


        List<Email> results =
                mailMate.getImportantEmails();


        displayedEmails.setAll(
                organizeInbox(
                        results
                )
        );


        pageTitle.setText(
                "Important"
        );


        statusLabel.setText(
                results.size()
                        + " important emails"
        );


        disableBulkSpamButton();


        selectFirstResult();
    }


    // =====================================================
    // SPAM VIEW
    // =====================================================

    @FXML
    private void showSpam() {

        if (mailMate == null) {

            return;
        }


        currentView =
                ViewMode.SPAM;


        List<Email> results =
                mailMate.getSpamEmails();


        displayedEmails.setAll(
                sortNewestFirst(
                        results
                )
        );


        pageTitle.setText(
                "Possible Spam"
        );


        statusLabel.setText(
                results.size()
                        + " possible spam emails"
        );


        /*
         * ONLY the Spam view can enable this button.
         */
        if (deleteSpamButton != null) {

            deleteSpamButton.setDisable(
                    results.isEmpty()
            );
        }


        selectFirstResult();
    }


    // =====================================================
    // PROMOTIONAL VIEW
    // =====================================================

    @FXML
    private void showPromotional() {

        if (mailMate == null) {

            return;
        }


        currentView =
                ViewMode.PROMOTIONAL;


        List<Email> results =
                mailMate.getPromotionalEmails();


        displayedEmails.setAll(
                sortNewestFirst(
                        results
                )
        );


        pageTitle.setText(
                "Promotions"
        );


        statusLabel.setText(
                results.size()
                        + " promotional emails"
        );


        disableBulkSpamButton();

        updatePromotionalBulkButton(
                !results.isEmpty()
        );

        selectFirstResult();
    }


    // =====================================================
    // SEARCH
    // =====================================================

    @FXML
    private void handleSearch() {

        if (mailMate == null
                || searchField == null) {

            return;
        }


        String keyword =
                searchField
                        .getText()
                        .trim();


        if (keyword.isBlank()) {

            showInbox();

            return;
        }


        currentView =
                ViewMode.SEARCH;


        List<Email> results =
                mailMate.search(
                        keyword
                );


        displayedEmails.setAll(
                organizeInbox(
                        results
                )
        );


        pageTitle.setText(
                "Search"
        );


        statusLabel.setText(
                results.size()
                        + " result(s) for \""
                        + keyword
                        + "\""
        );


        disableBulkSpamButton();


        selectFirstResult();
    }


    // =====================================================
    // SUMMARY
    // =====================================================

    /**
     * Displays the selected email's summary.
     */
    @FXML
    private void handleShowSummary() {

        Email email =
                getSelectedEmail();


        if (email == null) {

            showInformation(
                    "No Email Selected",
                    "Select an email first."
            );

            return;
        }


        String summary =
                email.getSummary();


        /*
         * Generate on demand if necessary.
         */
        if (summary == null
                || summary.isBlank()) {

            summary =
                    mailMate.summarize(
                            email
                    );
        }


        summaryArea.setText(
                summary
        );


        statusLabel.setText(
                "Summary displayed"
        );
    }


    // =====================================================
    // STAR / UNSTAR
    // =====================================================

    /**
     * Stars or unstars the selected email in Gmail.
     *
     * Gmail/network work runs on a background thread so the
     * JavaFX Application Thread remains responsive.
     */
    @FXML
    private void handleStarToggle() {

        Email email =
                getSelectedEmail();

        if (email == null) {

            showInformation(
                    "No Email Selected",
                    "Select an email first."
            );

            return;
        }

        boolean shouldStar =
                !email.isStarred();

        if (starButton != null) {
            starButton.setDisable(true);
        }

        Task<Void> task =
                new Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        if (shouldStar) {

                            mailMate.star(
                                    email
                            );

                        } else {

                            mailMate.unstar(
                                    email
                            );
                        }

                        return null;
                    }
                };

        task.setOnSucceeded(event -> {

            if (starButton != null) {

                starButton.setDisable(false);

                starButton.setText(
                        email.isStarred()
                                ? "★ Unstar"
                                : "☆ Star"
                );
            }

            emailListView.refresh();

            updateCounters();

            if (email.isStarred()) {

                statusLabel.setText(
                        "Email starred"
                );

            } else {

                statusLabel.setText(
                        "Email unstarred"
                );

                if (currentView ==
                        ViewMode.STARRED) {

                    displayedEmails.remove(
                            email
                    );

                    clearEmailDetails();

                    selectFirstResult();
                }
            }
        });

        task.setOnFailed(event -> {

            if (starButton != null) {

                starButton.setDisable(false);

                starButton.setText(
                        email.isStarred()
                                ? "★ Unstar"
                                : "☆ Star"
                );
            }

            Throwable error =
                    task.getException();

            showError(
                    shouldStar
                            ? "Unable to Star Email"
                            : "Unable to Unstar Email",
                    getErrorMessage(
                            error instanceof Exception
                                    ? (Exception) error
                                    : new Exception(error)
                    )
            );
        });

        Thread thread =
                new Thread(
                        task,
                        "MailMate-Star-Toggle"
                );

        thread.setDaemon(true);
        thread.start();
    }


    // =====================================================
    // MARK AS SPAM
    // =====================================================

    /**
     * Marks the selected sender as a spam source.
     */
    @FXML
    private void handleMarkAsSpam() {

        Email email =
                getSelectedEmail();


        if (email == null) {

            showInformation(
                    "No Email Selected",
                    "Select an email first."
            );

            return;
        }


        if (mailMate.isSenderMarkedAsSpam(
                email
        )) {

            showInformation(
                    "Already Marked as Spam",
                    "This sender is already marked as a spam source."
            );

            return;
        }


        String sender =
                safe(
                        email.getSender()
                );


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Mark as Spam"
        );


        confirmation.setHeaderText(
                "Mark this sender as spam?"
        );


        confirmation.setContentText(
                sender
                        + "\n\n"
                        + "The current email and future emails "
                        + "from this sender will be classified "
                        + "as spam during this session."
        );


        confirmation
                .getButtonTypes()
                .setAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );


        confirmation
                .showAndWait()
                .ifPresent(result -> {

                    if (result == ButtonType.OK) {

                        mailMate.markAsSpam(
                                email
                        );


                        emailListView.refresh();


                        updateCounters();


                        statusLabel.setText(
                                "Sender marked as spam"
                        );


                        /*
                         * If currently looking at Spam,
                         * refresh the view so it stays correct.
                         */
                        if (currentView
                                == ViewMode.SPAM) {

                            showSpam();
                        }
                    }
                });
    }


    // =====================================================
    // ARCHIVE
    // =====================================================

    @FXML
    private void handleArchive() {

        Email email =
                getSelectedEmail();


        if (email == null) {

            showInformation(
                    "No Email Selected",
                    "Select an email first."
            );

            return;
        }


        try {

            mailMate.archive(
                    email
            );


            displayedEmails.remove(
                    email
            );


            clearEmailDetails();


            updateCounters();


            statusLabel.setText(
                    "Email archived"
            );

        } catch (Exception e) {

            showError(
                    "Unable to Archive Email",
                    getErrorMessage(e)
            );
        }
    }


    // =====================================================
    // DELETE SELECTED EMAIL
    // =====================================================

    @FXML
    private void handleDelete() {

        Email email =
                getSelectedEmail();


        if (email == null) {

            showInformation(
                    "No Email Selected",
                    "Select an email first."
            );

            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Move to Trash"
        );


        confirmation.setHeaderText(
                "Move this email to Trash?"
        );


        confirmation.setContentText(
                safe(
                        email.getSubject()
                )
        );


        confirmation
                .getButtonTypes()
                .setAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );


        confirmation
                .showAndWait()
                .ifPresent(result -> {

                    if (result == ButtonType.OK) {

                        try {

                            mailMate.moveToTrash(
                                    email
                            );


                            displayedEmails.remove(
                                    email
                            );


                            clearEmailDetails();


                            updateCounters();


                            statusLabel.setText(
                                    "Email moved to Trash"
                            );

                        } catch (Exception e) {

                            showError(
                                    "Unable to Move Email",
                                    getErrorMessage(e)
                            );
                        }
                    }
                });
    }


    // =====================================================
    // DELETE ALL SPAM
    // =====================================================

    /**
     * Moves all detected spam into Gmail Trash.
     *
     * This operation is deliberately restricted to
     * the Spam view.
     */
    @FXML
    private void handleDeleteAllSpam() {

        if (mailMate == null) {

            return;
        }


        /*
         * Safety guard.
         */
        if (currentView != ViewMode.SPAM) {

            showInformation(
                    "Spam Section Required",
                    "Open Possible Spam before using Delete All Spam."
            );

            return;
        }


        List<Email> spamEmails =
                mailMate.getSpamEmails();


        if (spamEmails.isEmpty()) {

            showInformation(
                    "No Spam",
                    "There are no emails currently classified as spam."
            );

            disableBulkSpamButton();

            return;
        }


        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        confirmation.setTitle(
                "Delete All Spam"
        );


        confirmation.setHeaderText(
                "Move all detected spam to Trash?"
        );


        confirmation.setContentText(
                spamEmails.size()
                        + " email(s) will be moved to Gmail Trash.\n\n"
                        + "They will not be permanently deleted."
        );


        confirmation
                .getButtonTypes()
                .setAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );


        confirmation
                .showAndWait()
                .ifPresent(result -> {

                    if (result == ButtonType.OK) {

                        try {

                            int count =
                                    spamEmails.size();


                            mailMate.deleteAllSpam();


                            displayedEmails.clear();


                            clearEmailDetails();


                            disableBulkSpamButton();


                            updateCounters();


                            statusLabel.setText(
                                    count
                                            + " spam email(s) moved to Trash"
                            );

                        } catch (Exception e) {

                            showError(
                                    "Unable to Delete Spam",
                                    getErrorMessage(e)
                            );
                        }
                    }
                });
    }


    // =====================================================
    // DELETE ALL PROMOTIONS
    // =====================================================

    /**
     * Moves all currently classified promotional emails
     * to Gmail Trash.
     *
     * This action is deliberately restricted to the
     * Promotions view, just like Delete All Spam is
     * restricted to the Spam view.
     */
    @FXML
    private void handleDeleteAllPromotions() {

        if (mailMate == null) {
            return;
        }

        /*
         * Safety guard: the bulk promotional action must
         * only be usable while the Promotions section is open.
         */
        if (currentView != ViewMode.PROMOTIONAL) {

            showInformation(
                    "Promotions Section Required",
                    "Open Promotions before using Clear All Promotions."
            );

            return;
        }

        List<Email> promotionalEmails =
                mailMate.getPromotionalEmails();

        if (promotionalEmails == null
                || promotionalEmails.isEmpty()) {

            showInformation(
                    "No Promotional Emails",
                    "There are no emails currently classified as promotional."
            );

            updatePromotionalBulkButton(false);
            return;
        }

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Clear All Promotions"
        );

        confirmation.setHeaderText(
                "Move all promotional emails to Trash?"
        );

        confirmation.setContentText(
                promotionalEmails.size()
                        + " email(s) will be moved to Gmail Trash.\n\n"
                        + "They will not be permanently deleted."
        );

        confirmation
                .getButtonTypes()
                .setAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );

        confirmation
                .showAndWait()
                .ifPresent(result -> {

                    if (result == ButtonType.OK) {

                        if (!loadingEmails.compareAndSet(false, true)) {
                            showInformation(
                                    "Operation In Progress",
                                    "Please wait for the current Gmail operation to finish."
                            );
                            return;
                        }

                        setLoadingState(
                                    true,
                                    "Clearing promotional emails..."
                            );

                            deletePromotionalButton.setDisable(true);

                            Task<Integer> task = new Task<>() {

                                @Override
                                protected Integer call() throws Exception {
                                    return mailMate
                                            .deleteAllPromotionalEmails();
                                }
                            };

                            task.setOnSucceeded(event -> {

                                int count = task.getValue();

                                /*
                                 * MailMate has already removed the
                             * promotional emails from its local
                             * email list. Clear the currently
                             * displayed Promotions view as well.
                             */
                            displayedEmails.clear();

                            clearEmailDetails();

                            updatePromotionalBulkButton(false);

                            updateCounters();

                                statusLabel.setText(
                                        count
                                                + " promotional email(s) moved to Trash"
                                );

                                setLoadingState(false, null);
                                loadingEmails.set(false);
                            });

                            task.setOnFailed(event -> {

                                setLoadingState(false, null);
                                loadingEmails.set(false);

                                Throwable error = task.getException();

                                showError(
                                        "Unable to Clear Promotions",
                                        getErrorMessage(
                                                error instanceof Exception
                                                        ? (Exception) error
                                                        : new Exception(error)
                                        )
                                );

                                updatePromotionalBulkButton(
                                        !mailMate.getPromotionalEmails().isEmpty()
                                );
                            });

                            Thread thread = new Thread(
                                    task,
                                    "MailMate-Delete-Promotions"
                            );

                            thread.setDaemon(true);
                            thread.start();
                        }
                    });
    }


    /**
     * Enables/disables the Clear All Promotions button.
     *
     * The button is only active inside the Promotions view
     * and only when promotional emails are available.
     */
    private void updatePromotionalBulkButton(
            boolean hasPromotions) {

        if (deletePromotionalButton == null) {
            return;
        }

        deletePromotionalButton.setVisible(
                currentView == ViewMode.PROMOTIONAL
        );

        deletePromotionalButton.setManaged(
                currentView == ViewMode.PROMOTIONAL
        );

        deletePromotionalButton.setDisable(
                currentView != ViewMode.PROMOTIONAL
                        || !hasPromotions
        );
    }


    // =====================================================
    // DISPLAY SELECTED EMAIL
    // =====================================================

    private void displayEmail(
            Email email) {

        if (email == null) {

            return;
        }


        // =================================================
        // BASIC INFORMATION
        // =================================================

        subjectLabel.setText(
                safe(
                        email.getSubject()
                )
        );


        senderLabel.setText(
                "From: "
                        + safe(
                                email.getSender()
                        )
        );


        if (email.getReceivedDate() != null) {

            dateLabel.setText(
                    email.getReceivedDate()
                            .format(
                                    DATE_FORMATTER
                            )
            );

        } else {

            dateLabel.setText(
                    ""
            );
        }


        // =================================================
        // CATEGORY
        // =================================================

        /*
         * Keep the category explicitly labelled so it is not
         * confused with the trust score or other metadata.
         */
        String displayCategory;
        String categoryStyleClass;

        if (email.isSpam()) {

            displayCategory = "Possible Spam";
            categoryStyleClass = "category-spam";

        } else if (email.isPromotional()) {

            displayCategory = "Promotional";
            categoryStyleClass = "category-promotional";

        } else {

            displayCategory = safe(email.getCategory());
            categoryStyleClass = "category-normal";
        }

        categoryLabel.setText(
                "Category: " + displayCategory
        );

        categoryLabel.getStyleClass().removeAll(
                "category-spam",
                "category-promotional",
                "category-normal"
        );

        categoryLabel.getStyleClass().add(
                categoryStyleClass
        );


        // =================================================
        // TRUST
        // =================================================

        trustScoreLabel.setText(
                "Trust: "
                        + email.getTrustScore()
                        + "/100"
        );


        // =================================================
        // SUMMARY
        // =================================================

        summaryArea.setText(
                "Click \"Summary\" to view the email summary."
        );


        // =================================================
        // BODY
        // =================================================

        bodyArea.setText(
                safe(
                        email.getBody()
                )
        );


        // =================================================
        // BUTTON STATE
        // =================================================

        if (summaryButton != null) {

            summaryButton.setDisable(
                    false
            );
        }


        if (starButton != null) {

            starButton.setDisable(
                    false
            );

            starButton.setText(
                    email.isStarred()
                            ? "★ Unstar"
                            : "☆ Star"
            );
        }


        if (markSpamButton != null) {

            markSpamButton.setDisable(
                    false
            );
        }


        if (archiveButton != null) {

            archiveButton.setDisable(
                    false
            );
        }


        if (deleteButton != null) {

            deleteButton.setDisable(
                    false
            );
        }


        // =================================================
        // MARK READ
        // =================================================

        if (!email.isRead()) {

            try {

                mailMate.markAsRead(
                        email
                );

                email.markAsRead();


                emailListView.refresh();


                updateCounters();

            } catch (Exception e) {

                statusLabel.setText(
                        "Could not mark email as read"
                );
            }
        }


        // =================================================
        // ATTACHMENTS
        // =================================================

        loadAttachmentsForEmail(
                email
        );
    }


    // =====================================================
    // ATTACHMENT LOADING
    // =====================================================

    /**
     * Downloads attachments for the selected email
     * into MailMate's local attachment directory and
     * puts them into the attachment list.
     */
    private void loadAttachmentsForEmail(
            Email email) {

        selectedAttachments.clear();

        if (email == null
                || !email.hasAttachment()
                || mailMate == null) {

            attachmentLabel.setText("");
            updateAttachmentButtons(null);
            return;
        }

        attachmentLabel.setText("Loading attachments...");
        updateAttachmentButtons(null);

        Task<List<File>> task = new Task<>() {

            @Override
            protected List<File> call() throws Exception {
                /*
                 * Attachment downloads can involve Gmail/network I/O.
                 * Keep that work off the JavaFX Application Thread.
                 */
                return mailMate.getAttachmentFiles(email);
            }
        };

        task.setOnSucceeded(event -> {

            List<File> files = task.getValue();

            selectedAttachments.setAll(files);

            attachmentLabel.setText(
                    files.size()
                            + " attachment"
                            + (files.size() == 1 ? "" : "s")
            );

            updateAttachmentButtons(
                    attachmentListView
                            .getSelectionModel()
                            .getSelectedItem()
            );
        });

        task.setOnFailed(event -> {

            attachmentLabel.setText(
                    "Attachment loading failed"
            );

            updateAttachmentButtons(null);
        });

        Thread thread = new Thread(
                task,
                "MailMate-Attachment-Loader"
        );

        thread.setDaemon(true);
        thread.start();
    }


    // =====================================================
    // DOWNLOAD ATTACHMENT
    // =====================================================

    @FXML
    private void handleDownloadAttachment() {

        File attachment =
                attachmentListView
                        .getSelectionModel()
                        .getSelectedItem();


        if (attachment == null) {

            showInformation(
                    "No Attachment Selected",
                    "Select an attachment first."
            );

            return;
        }


        FileChooser fileChooser =
                new FileChooser();


        fileChooser.setTitle(
                "Save Attachment"
        );


        fileChooser.setInitialFileName(
                attachment.getName()
        );


        File destination =
                fileChooser.showSaveDialog(
                        emailListView
                                .getScene()
                                .getWindow()
                );


        if (destination == null) {

            return;
        }


        try {

            Files.copy(
                    attachment.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );


            statusLabel.setText(
                    "Attachment saved: "
                            + destination.getName()
            );

        } catch (IOException e) {

            showError(
                    "Unable to Download Attachment",
                    getErrorMessage(e)
            );
        }
    }


    // =====================================================
    // EXTRACT ATTACHMENT TEXT
    // =====================================================

    @FXML
    private void handleExtractAttachment() {

        File attachment =
                attachmentListView
                        .getSelectionModel()
                        .getSelectedItem();


        if (attachment == null) {

            showInformation(
                    "No Attachment Selected",
                    "Select an attachment first."
            );

            return;
        }


        try {

            String text =
                    mailMate.extractAttachmentText(
                            attachment
                    );


            if (text == null
                    || text.isBlank()) {

                summaryArea.setText(
                        "No text could be extracted from this attachment."
                );

            } else {

                summaryArea.setText(
                        text
                );
            }


            statusLabel.setText(
                    "Attachment text extracted"
            );

        } catch (Exception e) {

            showError(
                    "Unable to Extract Attachment",
                    getErrorMessage(e)
            );
        }
    }


    // =====================================================
    // ATTACHMENT BUTTON STATE
    // =====================================================

    private void updateAttachmentButtons(
            File selectedFile) {

        boolean hasSelection =
                selectedFile != null;


        if (downloadAttachmentButton != null) {

            downloadAttachmentButton.setDisable(
                    !hasSelection
            );
        }


        if (extractAttachmentButton != null) {

            boolean supported =
                    hasSelection
                            && mailMate != null
                            && mailMate
                                    .isAttachmentSupported(
                                            selectedFile
                                    );


            extractAttachmentButton.setDisable(
                    !supported
            );
        }
    }


    // =====================================================
    // CLEAR EMAIL DETAILS
    // =====================================================

    private void clearEmailDetails() {

        senderLabel.setText(
                ""
        );


        subjectLabel.setText(
                ""
        );


        dateLabel.setText(
                ""
        );


        categoryLabel.setText(
                ""
        );

        categoryLabel.getStyleClass().removeAll(
                "category-spam",
                "category-promotional",
                "category-normal"
        );


        trustScoreLabel.setText(
                ""
        );


        attachmentLabel.setText(
                ""
        );


        summaryArea.setText(
                ""
        );


        bodyArea.clear();


        selectedAttachments.clear();


        updateAttachmentButtons(
                null
        );


        if (summaryButton != null) {

            summaryButton.setDisable(
                    true
            );
        }


        if (starButton != null) {

            starButton.setDisable(
                    true
            );

            starButton.setText(
                    "☆ Star"
            );
        }


        if (markSpamButton != null) {

            markSpamButton.setDisable(
                    true
            );
        }


        if (archiveButton != null) {

            archiveButton.setDisable(
                    true
            );
        }


        if (deleteButton != null) {

            deleteButton.setDisable(
                    true
            );
        }
    }


    // =====================================================
    // COUNTERS
    // =====================================================

    private void updateCounters() {

        if (mailMate == null) {

            return;
        }


        List<Email> currentEmails =
                mailMate.getEmails();


        int unread =
                0;


        for (Email email :
                currentEmails) {

            if (email != null
                    && !email.isRead()) {

                unread++;
            }
        }


        if (emailCountLabel != null) {

            emailCountLabel.setText(
                    String.valueOf(
                            currentEmails.size()
                    )
            );
        }


        if (unreadCountLabel != null) {

            unreadCountLabel.setText(
                    String.valueOf(
                            unread
                    )
            );
        }


        if (spamCountLabel != null) {

            spamCountLabel.setText(
                    String.valueOf(
                            mailMate.getSpamCount()
                    )
            );
        }


        if (promotionalCountLabel != null) {

            promotionalCountLabel.setText(
                    String.valueOf(
                            mailMate.getPromotionalCount()
                    )
            );
        }
    }


    // =====================================================
    // ACCOUNT
    // =====================================================

    private void updateAccountInformation() {

        if (mailMate == null
                || accountLabel == null) {

            return;
        }


        String account =
                mailMate.getAccountEmail();


        if (account == null
                || account.isBlank()) {

            accountLabel.setText(
                    "Not connected"
            );

        } else {

            accountLabel.setText(
                    account
            );
        }
    }


    // =====================================================
    // SORTING
    // =====================================================

    private List<Email> sortNewestFirst(
            List<Email> emails) {

        List<Email> sorted =
                new ArrayList<>();


        if (emails != null) {

            for (Email email :
                    emails) {

                if (email != null) {

                    sorted.add(
                            email
                    );
                }
            }
        }


        sorted.sort(
                Comparator.comparing(
                        Email::getReceivedDate,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );


        return sorted;
    }


    // =====================================================
    // SELECTED EMAIL
    // =====================================================

    private Email getSelectedEmail() {

        return emailListView
                .getSelectionModel()
                .getSelectedItem();
    }


    private void selectFirstResult() {

        if (displayedEmails.isEmpty()) {

            clearEmailDetails();

            return;
        }


        emailListView
                .getSelectionModel()
                .select(
                        0
                );
    }


    // =====================================================
    // SPAM BUTTON
    // =====================================================

    private void disableBulkSpamButton() {

        if (deleteSpamButton != null) {

            deleteSpamButton.setDisable(
                    true
            );
        }

        updatePromotionalBulkButton(false);
    }


    /**
     * Updates controls that indicate whether Gmail loading is in progress.
     * This method is always called from the JavaFX Application Thread.
     */
    private void setLoadingState(
            boolean loading,
            String message) {

        if (statusLabel != null && message != null) {
            statusLabel.setText(message);
        }

        if (summaryButton != null) {
            summaryButton.setDisable(loading);
        }

        if (markSpamButton != null) {
            markSpamButton.setDisable(loading);
        }

        if (archiveButton != null) {
            archiveButton.setDisable(loading);
        }

        if (deleteButton != null) {
            deleteButton.setDisable(loading);
        }

        if (downloadAttachmentButton != null) {
            downloadAttachmentButton.setDisable(loading);
        }

        if (extractAttachmentButton != null) {
            extractAttachmentButton.setDisable(loading);
        }

        if (deletePromotionalButton != null) {
            deletePromotionalButton.setDisable(
                    loading
                            || currentView != ViewMode.PROMOTIONAL
                            || mailMate == null
                            || mailMate.getPromotionalEmails().isEmpty()
            );
        }

        if (changeAccountButton != null) {
            changeAccountButton.setDisable(loading);
        }
    }


    // =====================================================
    // ERROR DIALOG
    // =====================================================

    private void showError(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );


        alert.setTitle(
                title
        );


        alert.setHeaderText(
                null
        );


        alert.setContentText(
                message == null
                        || message.isBlank()
                        ? "Unknown error."
                        : message
        );


        alert.showAndWait();
    }


    // =====================================================
    // INFORMATION DIALOG
    // =====================================================

    private void showInformation(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );


        alert.setTitle(
                title
        );


        alert.setHeaderText(
                null
        );


        alert.setContentText(
                message
        );


        alert.showAndWait();
    }


    // =====================================================
    // ERROR MESSAGE
    // =====================================================

    private String getErrorMessage(
            Exception exception) {

        if (exception == null) {

            return "Unknown error.";
        }


        String message =
                exception.getMessage();


        if (message == null
                || message.isBlank()) {

            return exception
                    .getClass()
                    .getSimpleName();
        }


        return message;
    }


    // =====================================================
    // SAFE TEXT
    // =====================================================

    private String safe(
            String value) {

        if (value == null
                || value.isBlank()) {

            return "(empty)";
        }


        return value;
    }
}