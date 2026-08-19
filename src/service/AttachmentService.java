package service;

import extractor.ImageExtractor;
import extractor.PDFExtractor;
import extractor.WordExtractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles local attachment processing.
 *
 * Responsibilities:
 *
 * - Detect supported attachment formats
 * - Extract text from one attachment
 * - Extract text from multiple attachments
 * - Provide basic attachment metadata
 *
 * Gmail downloading remains the responsibility of MailService.
 */
public class AttachmentService {

    // =====================================================
    // SUPPORTED EXTENSIONS
    // =====================================================

    private static final String[] PDF_EXTENSIONS = {
            ".pdf"
    };

    private static final String[] WORD_EXTENSIONS = {
            ".doc",
            ".docx"
    };

    private static final String[] IMAGE_EXTENSIONS = {
            ".png",
            ".jpg",
            ".jpeg",
            ".bmp",
            ".gif",
            ".tif",
            ".tiff"
    };


    // =====================================================
    // SINGLE FILE EXTRACTION
    // =====================================================

    /**
     * Extracts text from a supported attachment.
     *
     * @param file attachment file
     * @return extracted text
     * @throws IOException if the file cannot be processed
     */
    public String extractText(
            File file)
            throws IOException {

        validateFile(file);

        String fileName =
                file.getName()
                        .toLowerCase();

        if (isPdf(fileName)) {

            return PDFExtractor.extractText(
                    file.getAbsolutePath()
            );
        }

        if (isWord(fileName)) {

            return WordExtractor.extractText(
                    file.getAbsolutePath()
            );
        }

        if (isImage(fileName)) {

            return ImageExtractor.extractText(
                    file.getAbsolutePath()
            );
        }

        throw new IllegalArgumentException(
                "Unsupported attachment format: "
                        + file.getName()
        );
    }


    // =====================================================
    // MULTIPLE FILE EXTRACTION
    // =====================================================

    /**
     * Extracts text from all supported files
     * in a list.
     *
     * Unsupported files are skipped.
     *
     * @param files attachment files
     * @return extracted text for each supported file
     * @throws IOException if a supported file cannot be read
     */
    public List<String> extractText(
            List<File> files)
            throws IOException {

        if (files == null
                || files.isEmpty()) {

            return new ArrayList<>();
        }

        List<String> extractedText =
                new ArrayList<>();

        for (File file : files) {

            if (file == null
                    || !isSupported(file)) {

                continue;
            }

            extractedText.add(
                    extractText(file)
            );
        }

        return extractedText;
    }


    // =====================================================
    // SUPPORTED FORMAT CHECK
    // =====================================================

    /**
     * Determines whether the supplied file format
     * is supported by MailMate.
     */
    public boolean isSupported(
            File file) {

        if (file == null
                || file.getName() == null) {

            return false;
        }

        String fileName =
                file.getName()
                        .toLowerCase();

        return isPdf(fileName)
                || isWord(fileName)
                || isImage(fileName);
    }


    /**
     * Returns the type of a supported attachment.
     *
     * Possible values:
     *
     * PDF
     * WORD
     * IMAGE
     * UNKNOWN
     */
    public String getAttachmentType(
            File file) {

        if (file == null
                || file.getName() == null) {

            return "UNKNOWN";
        }

        String fileName =
                file.getName()
                        .toLowerCase();

        if (isPdf(fileName)) {
            return "PDF";
        }

        if (isWord(fileName)) {
            return "WORD";
        }

        if (isImage(fileName)) {
            return "IMAGE";
        }

        return "UNKNOWN";
    }


    /**
     * Returns the file extension.
     */
    public String getFileExtension(
            File file) {

        if (file == null
                || file.getName() == null) {

            return "";
        }

        String fileName =
                file.getName();

        int dotIndex =
                fileName.lastIndexOf('.');

        if (dotIndex < 0
                || dotIndex == fileName.length() - 1) {

            return "";
        }

        return fileName
                .substring(dotIndex + 1)
                .toLowerCase();
    }


    /**
     * Returns the file size in bytes.
     */
    public long getFileSize(
            File file)
            throws IOException {

        validateFile(file);

        return file.length();
    }


    /**
     * Returns a human-readable file size.
     */
    public String getFormattedFileSize(
            File file)
            throws IOException {

        long bytes =
                getFileSize(file);

        if (bytes < 1024) {

            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {

            return String.format(
                    "%.1f MB",
                    bytes / (
                            1024.0
                                    * 1024.0
                    )
            );
        }

        return String.format(
                "%.1f GB",
                bytes / (
                        1024.0
                                * 1024.0
                                * 1024.0
                )
        );
    }


    // =====================================================
    // EXTRACTION HELPERS
    // =====================================================

    private boolean isPdf(
            String fileName) {

        return hasExtension(
                fileName,
                PDF_EXTENSIONS
        );
    }


    private boolean isWord(
            String fileName) {

        return hasExtension(
                fileName,
                WORD_EXTENSIONS
        );
    }


    private boolean isImage(
            String fileName) {

        return hasExtension(
                fileName,
                IMAGE_EXTENSIONS
        );
    }


    private boolean hasExtension(
            String fileName,
            String[] extensions) {

        if (fileName == null) {
            return false;
        }

        for (String extension :
                extensions) {

            if (fileName.endsWith(
                    extension
            )) {

                return true;
            }
        }

        return false;
    }


    // =====================================================
    // VALIDATION
    // =====================================================

    private void validateFile(
            File file)
            throws IOException {

        if (file == null) {

            throw new IllegalArgumentException(
                    "Attachment file cannot be null."
            );
        }

        if (!file.exists()) {

            throw new IOException(
                    "Attachment file does not exist: "
                            + file.getAbsolutePath()
            );
        }

        if (!file.isFile()) {

            throw new IOException(
                    "Attachment path does not point "
                            + "to a file: "
                            + file.getAbsolutePath()
            );
        }
    }
}
