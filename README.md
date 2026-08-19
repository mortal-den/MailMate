📬 MailMate
MailMate is a Java desktop email-management application built around Gmail. It is designed to reduce inbox overload by helping users organize, inspect, classify, and manage emails from a JavaFX interface.
The current version focuses on practical email management, local analysis, attachment handling, spam/promotional organization, and Gmail account management.

---

✨ Features
📥 Email Management
Gmail inbox integration through Google OAuth
Inbox organization using unread, important, starred, attachment, spam, and promotional signals
Search emails
Read and inspect selected emails
Mark emails as read/unread
Star and unstar emails
Mark emails as important
Archive selected emails
Delete selected emails
Refresh inbox
Dedicated views for:
Inbox
Unread
Starred
Important
Spam
Promotional
🛡️ Spam & Promotional Management
Spam classification
Promotional classification
Mark selected emails as spam
Delete all spam from the Spam view
Clear all promotional emails from the Promotional view
Spam sensitivity setting
Important sender management
Bulk Gmail operations are executed asynchronously so the JavaFX interface remains responsive during longer network operations.
🧠 Email Analysis
Email category classification
Trust score calculation
Local email summaries
Daily inbox brief
Combined category, trust, summary, and spam/promotional signals
Important-sender awareness
📎 Attachment Processing
Detect email attachments
Display attachment metadata
Download attachments
PDF text extraction
Microsoft Word document text extraction
Image OCR/text extraction
👤 Account Management
Google OAuth authentication
Detect the authenticated Gmail account
Change Gmail account
Fresh OAuth flow when changing accounts
🖥️ JavaFX Desktop UI
JavaFX graphical interface
Organized inbox presentation
Category and trust metadata
Star/unstar controls
Attachment controls
Loading states and status feedback
Background tasks for Gmail/network operations

---

🏗️ Architecture
MailMate separates the graphical interface, application coordination, business services, and Gmail/local processing.

```text
┌─────────────────────────────┐
│        JavaFX GUI           │
│ main.fxml + style.css       │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│   MailMateController        │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│         MailMate            │
│ Application coordination    │
└──────────────┬──────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
┌──────────────┐  ┌──────────────────┐
│   Services   │  │    MailService   │
│              │  │                  │
│ Search       │  │ Gmail API        │
│ Category     │  │ OAuth            │
│ Spam         │  │ Email actions    │
│ Trust        │  │ Attachments      │
│ Summary      │  │ Gmail labels     │
│ Brief        │  └────────┬─────────┘
│ Attachment   │           │
└──────┬───────┘           ▼
       │             ┌───────────────┐
       └────────────►│ Gmail / Local │
                     │ Processing    │
                     └───────────────┘
```

## The `MailMate` application layer coordinates shared service instances, while the JavaFX controller communicates with `MailMate` rather than directly managing every service.

📁 Project Structure

```text
MailMate/
│
├── src/
│   ├── app/
│   │   ├── Main.java
│   │   ├── MailMate.java
│   │   └── MailMateApp.java
│   │
│   ├── model/
│   │   ├── Account.java
│   │   ├── Email.java
│   │   ├── Folder.java
│   │   └── Settings.java
│   │
│   ├── service/
│   │   ├── AttachmentService.java
│   │   ├── BriefService.java
│   │   ├── CategoryService.java
│   │   ├── MailService.java
│   │   ├── SearchService.java
│   │   ├── SpamService.java
│   │   ├── SummaryService.java
│   │   └── TrustService.java
│   │
│   ├── extractor/
│   │   ├── ImageExtractor.java
│   │   ├── PDFExtractor.java
│   │   └── WordExtractor.java
│   │
│   ├── ui/
│   │   ├── MailMateController.java
│   │   └── Menu.java
│   │
│   ├── util/
│   │   ├── Constants.java
│   │   ├── DateFormatter.java
│   │   ├── Logger.java
│   │   └── Validator.java
│   │
│   └── resources/
│       ├── main.fxml
│       └── style.css
│
├── pom.xml
├── README.md
└── .gitignore
```

---

🛠️ Technologies
Java 23
JavaFX 23.0.2
Maven
Gmail API
Google OAuth 2.0
Apache PDFBox
Apache POI
Tess4J
Log4j 2
The project is configured through Maven and uses `app.MailMateApp` as the JavaFX application entry point.

---

🔐 Gmail Authentication
MailMate uses Google OAuth to connect to Gmail.
The application requires a Google Cloud project with Gmail API access and OAuth credentials.
The local `credentials.json` file is intentionally excluded from Git through `.gitignore`.
OAuth tokens are stored locally in the `tokens/` directory and are also excluded from Git.
Important
Never commit:

```text
credentials.json
tokens/
.env
```

## to the repository.

▶️ Running MailMate
Requirements
Install:
JDK 23
Maven
A Google Cloud project with Gmail API enabled
OAuth credentials configured for the application
Build
From the project root:

```bash
mvn clean compile
```

Run

```bash
mvn javafx:run
```

The JavaFX Maven plugin is configured to launch:

```text
app.MailMateApp
```

---

🧪 Testing
The current implementation has been tested across the major application workflows, including:
Gmail authentication
Inbox loading and refresh
Search
Unread / starred / important views
Star / unstar
Spam classification and management
Promotional classification and bulk clearing
Archive and delete
Attachment download and extraction
Email summaries
Category and trust display
Change Account
Organized inbox display
Background Gmail operations
The project was also verified with:

```bash
mvn clean compile
```

## and the final implementation compiled successfully.

⚡ Performance & Responsiveness
Gmail and other network-dependent operations are kept off the JavaFX Application Thread where appropriate.
This includes operations such as:
Gmail startup/connection
Inbox loading
Account switching
Attachment loading
Bulk promotional deletion
Star/unstar operations
This prevents long Gmail API operations from blocking the graphical interface.

---

📌 Current Limitations
The current version intentionally loads a fixed initial set of emails rather than implementing mailbox pagination.
For large mailboxes, a future Load More / pagination system would allow older emails to be fetched on demand instead of increasing initial loading work.
Google OAuth verification has also not been treated as part of the current development build. The current project is intended for development/educational use.

---

🔮 Future Scope
Planned future improvements include:
📄 Load More / Pagination
Allow users to progressively load additional emails instead of relying on the current initial mailbox limit.
✅ Google OAuth Verification & Public Release
Complete the Google OAuth verification/release process required for wider public distribution and prepare MailMate for production use.
📱 Android Version
Develop a dedicated Android version of MailMate so the application's email-management and organization features can be used on mobile devices.
🔎 Advanced Search & Filtering
Provide richer search and filtering options for large mailboxes.
📊 Advanced Email Analytics
Expand the current category, trust, spam, and summary systems with additional analytics and insights.

---

🎯 Learning Objectives
MailMate was developed as a practical Java project to gain experience with:
Object-Oriented Programming
Java application architecture
JavaFX desktop development
Maven project management
REST/API integration
Gmail API integration
OAuth authentication
Multithreading and background tasks
File processing
PDF and Word document extraction
OCR
Software testing and debugging
Git and GitHub
Separation of concerns and service-based architecture

---

👨‍💻 Author
Daksh
MailMate is an educational/personal project focused on learning Java application development and building a practical email-management system.

---

📜 License
This project is currently intended for educational and personal use.
