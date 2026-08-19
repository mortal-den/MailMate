package model;

import java.time.LocalDateTime;

/**
 * Represents one email inside MailMate.
 *
 * The object stores the original email information together
 * with MailMate's locally calculated metadata.
 */
public class Email {

    // =====================================================
    // ORIGINAL EMAIL DATA
    // =====================================================

    private final String sender;
    private final String receiver;
    private final String subject;
    private final String body;
    private final LocalDateTime receivedDate;


    // =====================================================
    // MAILMATE ANALYSIS
    // =====================================================

    private String category;
    private String summary;
    private int trustScore;


    // =====================================================
    // EMAIL STATE
    // =====================================================

    private boolean spam;
    private boolean promotional;

    private boolean read;
    private boolean starred;
    private boolean important;
    private boolean hasAttachment;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public Email(
            String sender,
            String receiver,
            String subject,
            String body,
            LocalDateTime receivedDate) {

        this.sender = sender;
        this.receiver = receiver;
        this.subject = subject;
        this.body = body;
        this.receivedDate = receivedDate;

        this.category =
                "OTHER";

        this.summary =
                "";

        this.trustScore =
                0;

        this.spam =
                false;

        this.promotional =
                false;

        this.read =
                false;

        this.starred =
                false;

        this.important =
                false;

        this.hasAttachment =
                false;
    }


    // =====================================================
    // BASIC EMAIL INFORMATION
    // =====================================================

    public String getSender() {

        return sender;
    }


    public String getReceiver() {

        return receiver;
    }


    public String getSubject() {

        return subject;
    }


    public String getBody() {

        return body;
    }


    public LocalDateTime getReceivedDate() {

        return receivedDate;
    }


    // =====================================================
    // CATEGORY
    // =====================================================

    public String getCategory() {

        return category;
    }


    public void setCategory(
            String category) {

        this.category =
                category == null
                        ? "OTHER"
                        : category;
    }


    // =====================================================
    // SUMMARY
    // =====================================================

    public String getSummary() {

        return summary;
    }


    public void setSummary(
            String summary) {

        this.summary =
                summary == null
                        ? ""
                        : summary;
    }


    // =====================================================
    // TRUST SCORE
    // =====================================================

    public int getTrustScore() {

        return trustScore;
    }


    public void setTrustScore(
            int trustScore) {

        this.trustScore =
                Math.max(
                        0,
                        Math.min(
                                100,
                                trustScore
                        )
                );
    }


    // =====================================================
    // SPAM
    // =====================================================

    public boolean isSpam() {

        return spam;
    }


    public void setSpam(
            boolean spam) {

        this.spam =
                spam;

        /*
         * An email explicitly classified as spam should
         * not simultaneously appear as ordinary promotion.
         */
        if (spam) {

            this.promotional =
                    false;
        }
    }


    // =====================================================
    // PROMOTIONAL
    // =====================================================

    public boolean isPromotional() {

        return promotional;
    }


    public void setPromotional(
            boolean promotional) {

        /*
         * Spam takes precedence over promotion.
         */
        this.promotional =
                this.spam
                        ? false
                        : promotional;
    }


    // =====================================================
    // READ / UNREAD
    // =====================================================

    public boolean isRead() {

        return read;
    }


    public void markAsRead() {

        this.read =
                true;
    }


    public void markAsUnread() {

        this.read =
                false;
    }


    public void setRead(
            boolean read) {

        this.read =
                read;
    }


    // =====================================================
    // STARRED
    // =====================================================

    public boolean isStarred() {

        return starred;
    }


    public void setStarred(
            boolean starred) {

        this.starred =
                starred;
    }


    // =====================================================
    // IMPORTANT
    // =====================================================

    public boolean isImportant() {

        return important;
    }


    public void setImportant(
            boolean important) {

        this.important =
                important;
    }


    // =====================================================
    // ATTACHMENT
    // =====================================================

    public boolean hasAttachment() {

        return hasAttachment;
    }


    public void setHasAttachment(
            boolean hasAttachment) {

        this.hasAttachment =
                hasAttachment;
    }


    // =====================================================
    // STRING REPRESENTATION
    // =====================================================

    @Override
    public String toString() {

        return "Email {" +
                "\n  Sender       : " + sender +
                "\n  Receiver     : " + receiver +
                "\n  Subject      : " + subject +
                "\n  Received On  : " + receivedDate +
                "\n  Category     : " + category +
                "\n  Trust Score  : " + trustScore +
                "\n  Spam         : " + spam +
                "\n  Promotional  : " + promotional +
                "\n  Starred      : " + starred +
                "\n  Important    : " + important +
                "\n  Read         : " + read +
                "\n  Attachment   : " + hasAttachment +
                "\n}";
    }
}