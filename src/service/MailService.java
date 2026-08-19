package service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.auth.oauth2.StoredCredential;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;

import model.Email;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.security.GeneralSecurityException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;


/**
 * Handles all communication between MailMate
 * and the Gmail API.
 *
 * Responsibilities:
 *
 * - Gmail OAuth authentication
 * - Email retrieval
 * - Email state/action changes
 * - Archive and Trash operations
 * - Attachment downloading
 * - Gmail message parsing
 * - Mapping MailMate Email objects to Gmail IDs
 */
public class MailService {

    // =====================================================
    // APPLICATION / OAUTH CONFIGURATION
    // =====================================================

    private static final String APPLICATION_NAME =
            "MailMate";

    private static final JsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    private static final String CREDENTIALS_FILE_PATH =
            "/credentials.json";

    private static final String TOKENS_DIRECTORY_PATH =
            "tokens";

    /**
     * Gmail permission required by MailMate.
     *
     * This allows MailMate to:
     *
     * - read messages
     * - modify labels
     * - mark messages read/unread
     * - star/unstar
     * - mark important/not important
     * - archive
     * - move messages to Trash
     */
    private static final List<String> SCOPES =
            Collections.singletonList(
                    GmailScopes.GMAIL_MODIFY
            );

    /**
     * Local OAuth user identifier.
     *
     * This is NOT a Gmail address.
     */
    private static final String OAUTH_USER_ID =
            "mailmate-user";


    // =====================================================
    // ATTACHMENT CONFIGURATION
    // =====================================================

    private static final String ATTACHMENT_DIRECTORY =
            "mailmate-attachments";

    /**
     * Keep the number of queued message requests deliberately
     * below Gmail's maximum batch size. A smaller batch also
     * reduces the chance of "Too many concurrent requests for
     * user" errors when loading a mailbox after an account switch.
     */
    private static final int MESSAGE_BATCH_SIZE = 10;


    // =====================================================
    // RUNTIME STATE
    // =====================================================

    private Gmail gmailService;

    private String accountEmail;

    /**
     * Maps each Email object loaded by MailMate
     * to its actual Gmail message ID.
     */
    private final Map<Email, String> emailMessageIds =
            new IdentityHashMap<>();


    // =====================================================
    // CONNECTION
    // =====================================================

    /**
     * Connects MailMate to Gmail using OAuth.
     *
     * No Gmail address is hard-coded.
     * Google determines which account is authenticated.
     */
    public void connect()
            throws IOException,
            GeneralSecurityException {

        NetHttpTransport httpTransport =
                GoogleNetHttpTransport
                        .newTrustedTransport();

        Credential credential =
                getCredentials(
                        httpTransport
                );

        gmailService =
                new Gmail.Builder(
                        httpTransport,
                        JSON_FACTORY,
                        credential
                )
                .setApplicationName(
                        APPLICATION_NAME
                )
                .build();

        /*
         * Ask Gmail which account was actually
         * authenticated.
         */
        accountEmail =
                gmailService
                        .users()
                        .getProfile("me")
                        .execute()
                        .getEmailAddress();

        if (accountEmail == null
                || accountEmail.isBlank()) {

            gmailService = null;

            throw new IllegalStateException(
                    "Could not determine the "
                            + "authenticated Gmail account."
            );
        }
    }


    /**
     * Returns whether MailMate is connected to Gmail.
     */
    public boolean isConnected() {

        return gmailService != null;
    }


    /**
     * Returns the currently authenticated Gmail account.
     */
    public String getAccountEmail() {

        return accountEmail;
    }


    // =====================================================
    // EMAIL RETRIEVAL
    // =====================================================

    /**
     * Retrieves emails from the authenticated Gmail account.
     *
     * @param maxResults maximum number of messages to load
     * @return list of MailMate Email objects
     */
    public List<Email> fetchEmails(
            int maxResults)
            throws IOException {

        if (!isConnected()) {

            throw new IllegalStateException(
                    "MailMate is not connected to Gmail."
            );
        }

        if (maxResults <= 0) {

            throw new IllegalArgumentException(
                    "Maximum number of emails must be "
                            + "greater than zero."
            );
        }

        /*
         * Clear old message mappings because a new
         * mailbox retrieval is starting.
         */
        emailMessageIds.clear();

        ListMessagesResponse response =
                gmailService
                        .users()
                        .messages()
                        .list("me")
                        .setMaxResults(
                                (long) maxResults
                        )
                        .execute();

        List<Message> messageReferences =
                response.getMessages();

        if (messageReferences == null
                || messageReferences.isEmpty()) {

            return new ArrayList<>();
        }

        /*
         * The old implementation performed one HTTP request
         * per message, sequentially. With 100 messages this
         * creates a long chain of network round trips.
         *
         * Gmail supports batching multiple messages.get calls
         * into one HTTP request. We keep the full message format
         * because MailMate needs the body, labels and MIME
         * structure for summaries, spam/category analysis and
         * attachment detection.
         */
        List<Email> orderedResults =
                Collections.synchronizedList(
                        new ArrayList<>(
                                Collections.nCopies(
                                        messageReferences.size(),
                                        null
                                )
                        )
                );

        List<String> batchFailures =
                Collections.synchronizedList(
                        new ArrayList<>()
                );

        for (int start = 0;
                start < messageReferences.size();
                start += MESSAGE_BATCH_SIZE) {

            int end =
                    Math.min(
                            start + MESSAGE_BATCH_SIZE,
                            messageReferences.size()
                    );

            BatchRequest batch =
                    gmailService.batch();

            for (int index = start;
                    index < end;
                    index++) {

                Message messageReference =
                        messageReferences.get(index);

                if (messageReference == null
                        || messageReference.getId() == null) {

                    continue;
                }

                final int resultIndex = index;
                final String messageId =
                        messageReference.getId();

                gmailService
                        .users()
                        .messages()
                        .get(
                                "me",
                                messageId
                        )
                        .setFormat("FULL")
                        .setFields(
                                "id,internalDate,payload,labelIds"
                        )
                        .queue(
                                batch,
                                new JsonBatchCallback<Message>() {

                                    @Override
                                    public void onSuccess(
                                            Message message,
                                            HttpHeaders responseHeaders) {

                                        if (message == null) {
                                            batchFailures.add(
                                                    "Empty response for message "
                                                            + messageId
                                            );
                                            return;
                                        }

                                        Email email =
                                                convertToEmail(
                                                        message
                                                );

                                        orderedResults.set(
                                                resultIndex,
                                                email
                                        );

                                        synchronized (
                                                emailMessageIds
                                        ) {
                                            emailMessageIds.put(
                                                    email,
                                                    message.getId()
                                            );
                                        }
                                    }

                                    @Override
                                    public void onFailure(
                                            GoogleJsonError error,
                                            HttpHeaders responseHeaders) {

                                        String errorMessage =
                                                error == null
                                                        ? "Unknown Gmail API error"
                                                        : error.getMessage();

                                        batchFailures.add(
                                                "Message "
                                                        + messageId
                                                        + ": "
                                                        + errorMessage
                                        );
                                    }
                                }
                        );
            }

            /*
             * Executes at most MESSAGE_BATCH_SIZE messages in
             * one HTTP batch. Keeping this at 10 is intentional:
             * Gmail can reject a large number of simultaneous
             * per-user message requests with a
             * "Too many concurrent requests for user" error.
             */
            if (batch.size() > 0) {
                batch.execute();
            }
        }

        if (!batchFailures.isEmpty()) {

            throw new IOException(
                    "Some Gmail messages could not be loaded: "
                            + String.join(
                                    "; ",
                                    batchFailures
                            )
            );
        }

        /*
         * Remove null slots while preserving Gmail's original
         * newest-first ordering.
         */
        List<Email> emails =
                new ArrayList<>();

        for (Email email : orderedResults) {

            if (email != null) {
                emails.add(email);
            }
        }

        return emails;
    }


    // =====================================================
    // EMAIL ACTIONS
    // =====================================================

    /**
     * Marks an email as read.
     */
    public void markAsRead(
            Email email)
            throws IOException {

        modifyLabels(
                email,
                Collections.emptyList(),
                Collections.singletonList("UNREAD")
        );

        email.markAsRead();
    }


    /**
     * Marks an email as unread.
     */
    public void markAsUnread(
            Email email)
            throws IOException {

        modifyLabels(
                email,
                Collections.singletonList("UNREAD"),
                Collections.emptyList()
        );

        email.markAsUnread();
    }


    /**
     * Stars an email.
     */
    public void star(
            Email email)
            throws IOException {

        modifyLabels(
                email,
                Collections.singletonList("STARRED"),
                Collections.emptyList()
        );

        email.setStarred(true);
    }


    /**
     * Removes the star from an email.
     */
    public void unstar(
            Email email)
            throws IOException {

        modifyLabels(
                email,
                Collections.emptyList(),
                Collections.singletonList("STARRED")
        );

        email.setStarred(false);
    }


    /**
     * Marks an email as important.
     */
    public void markImportant(
            Email email)
            throws IOException {

        modifyLabels(
                email,
                Collections.singletonList("IMPORTANT"),
                Collections.emptyList()
        );

        email.setImportant(true);
    }


    /**
     * Removes the IMPORTANT label.
     */
    public void markNotImportant(
            Email email)
            throws IOException {

        modifyLabels(
                email,
                Collections.emptyList(),
                Collections.singletonList("IMPORTANT")
        );

        email.setImportant(false);
    }


    /**
     * Archives an email.
     *
     * Gmail archive is implemented by removing
     * the INBOX label.
     */
    public void archive(
            Email email)
            throws IOException {

        modifyLabels(
                email,
                Collections.emptyList(),
                Collections.singletonList("INBOX")
        );
    }


    /**
     * Moves one email to Gmail Trash.
     *
     * This is NOT permanent deletion.
     */
    public void moveToTrash(
            Email email)
            throws IOException {

        if (!isConnected()) {

            throw new IllegalStateException(
                    "MailMate is not connected to Gmail."
            );
        }

        String messageId =
                getMessageId(email);

        gmailService
                .users()
                .messages()
                .trash(
                        "me",
                        messageId
                )
                .execute();
    }


    /**
     * Moves multiple emails to Gmail Trash.
     *
     * This method is used by the
     * "Delete All Spam" feature.
     *
     * The messages are moved to Gmail Trash.
     * They are NOT permanently deleted.
     */
    public void moveEmailsToTrash(
            List<Email> emails)
            throws IOException {

        if (!isConnected()) {

            throw new IllegalStateException(
                    "MailMate is not connected to Gmail."
            );
        }

        if (emails == null
                || emails.isEmpty()) {

            return;
        }

        for (Email email :
                emails) {

            if (email == null) {
                continue;
            }

            moveToTrash(email);
        }
    }


    /**
     * Moves all promotional emails supplied by MailMate
     * to Gmail Trash.
     *
     * Promotional classification is performed locally by
     * CategoryService. This method only performs the Gmail
     * action on the already-classified Email objects.
     *
     * This is intentionally separate from moveEmailsToTrash()
     * so the application can clearly distinguish the
     * "Clear All Promotions" action from other bulk actions.
     *
     * @param promotionalEmails emails classified as promotional
     * @throws IOException if Gmail cannot move a message to Trash
     */
    public void movePromotionalEmailsToTrash(
            List<Email> promotionalEmails)
            throws IOException {

        if (!isConnected()) {

            throw new IllegalStateException(
                    "MailMate is not connected to Gmail."
            );
        }

        if (promotionalEmails == null
                || promotionalEmails.isEmpty()) {

            return;
        }

        for (Email email : promotionalEmails) {

            if (email == null) {
                continue;
            }

            moveToTrash(email);
        }
    }


    /**
     * Modifies Gmail labels for a message.
     */
    private void modifyLabels(
            Email email,
            List<String> labelsToAdd,
            List<String> labelsToRemove)
            throws IOException {

        if (!isConnected()) {

            throw new IllegalStateException(
                    "MailMate is not connected to Gmail."
            );
        }

        String messageId =
                getMessageId(email);

        ModifyMessageRequest request =
                new ModifyMessageRequest()
                        .setAddLabelIds(
                                labelsToAdd
                        )
                        .setRemoveLabelIds(
                                labelsToRemove
                        );

        gmailService
                .users()
                .messages()
                .modify(
                        "me",
                        messageId,
                        request
                )
                .execute();
    }


    /**
     * Retrieves the Gmail message ID associated
     * with a MailMate Email object.
     */
    private String getMessageId(
            Email email) {

        if (email == null) {

            throw new IllegalArgumentException(
                    "Email cannot be null."
            );
        }

        String messageId =
                emailMessageIds.get(email);

        if (messageId == null) {

            throw new IllegalArgumentException(
                    "The supplied Email object was not "
                            + "retrieved by this MailService instance."
            );
        }

        return messageId;
    }


    // =====================================================
    // ATTACHMENTS
    // =====================================================

    /**
     * Downloads all attachments from an email.
     *
     * The downloaded files are returned so that the
     * JavaFX GUI can display their names, sizes and
     * provide download/extraction actions.
     */
    public List<File> downloadAttachments(
            Email email)
            throws IOException {

        if (!isConnected()) {

            throw new IllegalStateException(
                    "MailMate is not connected to Gmail."
            );
        }

        String messageId =
                getMessageId(email);

        Message message =
                gmailService
                        .users()
                        .messages()
                        .get(
                                "me",
                                messageId
                        )
                        .setFormat("FULL")
                        .execute();

        Path attachmentDirectory =
                createAttachmentDirectory();

        List<File> files =
                new ArrayList<>();

        downloadAttachmentsFromPart(
                messageId,
                message.getPayload(),
                attachmentDirectory,
                files
        );

        return files;
    }


    /**
     * GUI-friendly alias for downloadAttachments().
     *
     * This allows the UI layer to request the
     * attachment files without knowing how Gmail
     * MIME processing works.
     */
    public List<File> getAttachmentFiles(
            Email email)
            throws IOException {

        return downloadAttachments(
                email
        );
    }


    /**
     * Downloads the attachments belonging to an email
     * and returns one requested attachment by filename.
     *
     * Returns null when the requested filename is not found.
     */
    public File downloadAttachment(
            Email email,
            String fileName)
            throws IOException {

        if (fileName == null
                || fileName.isBlank()) {

            throw new IllegalArgumentException(
                    "Attachment filename cannot be blank."
            );
        }

        List<File> files =
                downloadAttachments(
                        email
                );

        for (File file : files) {

            if (file != null
                    && file.getName()
                            .equalsIgnoreCase(
                                    fileName
                            )) {

                return file;
            }
        }

        return null;
    }


    /**
     * Recursively searches a MIME message structure
     * for attachments.
     */
    private void downloadAttachmentsFromPart(
            String messageId,
            MessagePart part,
            Path attachmentDirectory,
            List<File> files)
            throws IOException {

        if (part == null) {
            return;
        }

        String fileName =
                part.getFilename();

        /*
         * A non-empty filename indicates that this
         * MIME part represents an attachment.
         */
        if (fileName != null
                && !fileName.isBlank()) {

            byte[] attachmentData =
                    getAttachmentData(
                            messageId,
                            part
                    );

            if (attachmentData.length > 0) {

                String safeFileName =
                        createSafeFileName(
                                fileName
                        );

                Path outputPath =
                        createUniquePath(
                                attachmentDirectory,
                                safeFileName
                        );

                try (OutputStream outputStream =
                             Files.newOutputStream(
                                     outputPath,
                                     StandardOpenOption.CREATE_NEW
                             )) {

                    outputStream.write(
                            attachmentData
                    );
                }

                files.add(
                        outputPath.toFile()
                );
            }
        }

        /*
         * Multipart emails can contain nested parts,
         * so recursively inspect every child.
         */
        if (part.getParts() != null) {

            for (MessagePart child :
                    part.getParts()) {

                downloadAttachmentsFromPart(
                        messageId,
                        child,
                        attachmentDirectory,
                        files
                );
            }
        }
    }


    /**
     * Retrieves attachment bytes.
     *
     * Gmail can provide attachment data directly
     * or through a separate attachment ID.
     */
    private byte[] getAttachmentData(
            String messageId,
            MessagePart part)
            throws IOException {

        if (part.getBody() != null
                && part.getBody().getData() != null) {

            return Base64.getUrlDecoder()
                    .decode(
                            part.getBody().getData()
                    );
        }

        if (part.getBody() != null
                && part.getBody().getAttachmentId() != null) {

            String attachmentId =
                    part.getBody()
                            .getAttachmentId();

            MessagePartBody attachmentBody =
                    gmailService
                            .users()
                            .messages()
                            .attachments()
                            .get(
                                    "me",
                                    messageId,
                                    attachmentId
                            )
                            .execute();

            if (attachmentBody.getData() == null) {

                return new byte[0];
            }

            return Base64.getUrlDecoder()
                    .decode(
                            attachmentBody.getData()
                    );
        }

        return new byte[0];
    }


    /**
     * Creates the local attachment directory.
     */
    private Path createAttachmentDirectory()
            throws IOException {

        Path directory =
                Path.of(
                        ATTACHMENT_DIRECTORY
                );

        Files.createDirectories(
                directory
        );

        return directory;
    }


    /**
     * Prevents path traversal and invalid filename
     * characters from being used when saving attachments.
     */
    private String createSafeFileName(
            String fileName) {

        String name =
                Path.of(fileName)
                        .getFileName()
                        .toString();

        name = name.replaceAll(
                "[\\\\/:*?\"<>|]",
                "_"
        );

        if (name.isBlank()) {

            return "attachment";
        }

        return name;
    }


    /**
     * Creates a unique local path if a file with
     * the same name already exists.
     */
    private Path createUniquePath(
            Path directory,
            String fileName) {

        Path path =
                directory.resolve(
                        fileName
                );

        if (!Files.exists(path)) {

            return path;
        }

        String baseName =
                fileName;

        String extension = "";

        int dot =
                fileName.lastIndexOf('.');

        if (dot > 0) {

            baseName =
                    fileName.substring(
                            0,
                            dot
                    );

            extension =
                    fileName.substring(dot);
        }

        int counter = 1;

        while (Files.exists(path)) {

            path =
                    directory.resolve(
                            baseName
                                    + "_"
                                    + counter
                                    + extension
                    );

            counter++;
        }

        return path;
    }


    // =====================================================
    // OAUTH
    // =====================================================

    /**
     * Loads Google OAuth credentials and performs
     * the installed-application authorization flow.
     */
    private Credential getCredentials(
            NetHttpTransport httpTransport)
            throws IOException {

        InputStream inputStream =
                MailService.class
                        .getResourceAsStream(
                                CREDENTIALS_FILE_PATH
                        );

        if (inputStream == null) {

            throw new FileNotFoundException(
                    "Could not find "
                            + CREDENTIALS_FILE_PATH
            );
        }

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JSON_FACTORY,
                        new InputStreamReader(
                                inputStream
                        )
                );

        File tokenDirectory =
                new File(
                        TOKENS_DIRECTORY_PATH
                );

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport,
                        JSON_FACTORY,
                        clientSecrets,
                        SCOPES
                )
                .setDataStoreFactory(
                        new FileDataStoreFactory(
                                tokenDirectory
                        )
                )
                .setAccessType(
                        "offline"
                )
                .build();

        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder()
                        .setPort(8888)
                        .build();

        /*
         * This is a local OAuth credential-store key.
         * It is NOT the user's Gmail address.
         */
        return new AuthorizationCodeInstalledApp(
                flow,
                receiver
        ).authorize(
                OAUTH_USER_ID
        );
    }


    // =====================================================
    // MESSAGE CONVERSION
    // =====================================================

    /**
     * Converts a Gmail Message into a MailMate Email.
     */
    private Email convertToEmail(
            Message message) {

        MessagePart payload =
                message.getPayload();

        String sender =
                getHeader(
                        payload,
                        "From"
                );

        String receiver =
                getHeader(
                        payload,
                        "To"
                );

        String subject =
                getHeader(
                        payload,
                        "Subject"
                );

        String body =
                extractPlainText(
                        payload
                );

        LocalDateTime receivedDate =
                convertTimestamp(
                        message.getInternalDate()
                );

        Email email =
                new Email(
                        sender,
                        receiver,
                        subject,
                        body,
                        receivedDate
                );

        List<String> labelIds =
                message.getLabelIds();

        /*
         * MailMate defaults to read unless Gmail
         * explicitly reports the UNREAD label.
         */
        email.markAsRead();

        if (labelIds != null) {

            if (labelIds.contains(
                    "UNREAD"
            )) {

                email.markAsUnread();
            }

            email.setStarred(
                    labelIds.contains(
                            "STARRED"
                    )
            );

            email.setImportant(
                    labelIds.contains(
                            "IMPORTANT"
                    )
            );

        } else {

            email.setStarred(false);
            email.setImportant(false);
        }

        email.setHasAttachment(
                containsAttachment(
                        payload
                )
        );

        return email;
    }


    /**
     * Retrieves a specific Gmail message header.
     */
    private String getHeader(
            MessagePart payload,
            String headerName) {

        if (payload == null
                || payload.getHeaders() == null) {

            return "";
        }

        for (MessagePartHeader header :
                payload.getHeaders()) {

            if (headerName.equalsIgnoreCase(
                    header.getName()
            )) {

                return header.getValue() == null
                        ? ""
                        : header.getValue();
            }
        }

        return "";
    }


    /**
     * Extracts the first available text/plain
     * section from a MIME message.
     */
    private String extractPlainText(
            MessagePart part) {

        if (part == null) {
            return "";
        }

        if ("text/plain".equalsIgnoreCase(
                part.getMimeType()
        )) {

            if (part.getBody() == null
                    || part.getBody().getData() == null) {

                return "";
            }

            return decodeBase64Url(
                    part.getBody().getData()
            );
        }

        /*
         * If the current part isn't text/plain,
         * inspect nested MIME parts.
         */
        if (part.getParts() != null) {

            for (MessagePart child :
                    part.getParts()) {

                String text =
                        extractPlainText(
                                child
                        );

                if (!text.isBlank()) {

                    return text;
                }
            }
        }

        return "";
    }


    /**
     * Determines whether a MIME message contains
     * at least one attachment.
     */
    private boolean containsAttachment(
            MessagePart part) {

        if (part == null) {
            return false;
        }

        if (part.getFilename() != null
                && !part.getFilename().isBlank()) {

            return true;
        }

        if (part.getParts() != null) {

            for (MessagePart child :
                    part.getParts()) {

                if (containsAttachment(
                        child
                )) {

                    return true;
                }
            }
        }

        return false;
    }


    // =====================================================
    // UTILITY METHODS
    // =====================================================

    /**
     * Decodes Gmail's URL-safe Base64 encoded
     * message body.
     */
    private String decodeBase64Url(
            String encodedData) {

        byte[] decodedBytes =
                Base64.getUrlDecoder()
                        .decode(
                                encodedData
                        );

        return new String(
                decodedBytes,
                java.nio.charset.StandardCharsets.UTF_8
        );
    }


    /**
     * Converts Gmail's millisecond timestamp
     * into the computer's local date/time.
     */
    private LocalDateTime convertTimestamp(
            Long timestamp) {

        if (timestamp == null) {

            return LocalDateTime.now();
        }

        return Instant
                .ofEpochMilli(
                        timestamp
                )
                .atZone(
                        ZoneId.systemDefault()
                )
                .toLocalDateTime();
    }


    // =====================================================
    // ACCOUNT MANAGEMENT
    // =====================================================

    /**
     * Completely signs out the currently authorized MailMate
     * Google account and starts a fresh OAuth authorization flow.
     *
     * The saved OAuth credential for MailMate is removed from
     * the local token store before reconnecting. This prevents
     * MailMate from silently reusing the previous authorization.
     *
     * The Gmail account itself is never deleted or modified.
     *
     * @throws IOException if the OAuth token store cannot be
     *                     updated or the new authorization fails
     * @throws GeneralSecurityException if the secure HTTP
     *                                  transport cannot be created
     */
    public void changeAccount()
            throws IOException,
            GeneralSecurityException {

        /*
         * First disconnect the active Gmail service and clear
         * all runtime mappings.
         */
        disconnect();

        /*
         * Remove the locally stored OAuth credential associated
         * with MailMate's OAuth user key.
         *
         * This does not revoke access from Google's account.
         * It only removes MailMate's local saved credential,
         * causing the next connect() call to perform OAuth again.
         */
        File tokenDirectory =
                new File(
                        TOKENS_DIRECTORY_PATH
                );

        if (tokenDirectory.exists()) {

            NetHttpTransport httpTransport =
                    GoogleNetHttpTransport
                            .newTrustedTransport();

            DataStore<StoredCredential> credentialStore =
                    new FileDataStoreFactory(
                            tokenDirectory
                    ).getDataStore(
                            StoredCredential.DEFAULT_DATA_STORE_ID
                    );

            credentialStore.delete(
                    OAUTH_USER_ID
            );
        }

        /*
         * Start a completely fresh authorization flow.
         */
        connect();
    }


    // =====================================================
    // DISCONNECT
    // =====================================================

    /**
     * Clears the active Gmail connection and
     * associated runtime state.
     */
    public void disconnect() {

        gmailService = null;

        accountEmail = null;

        emailMessageIds.clear();
    }
}