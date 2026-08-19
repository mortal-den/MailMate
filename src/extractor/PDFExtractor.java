package extractor;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class PDFExtractor {

    private PDFExtractor() {
    }

    public static String extractText(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("PDF file path cannot be blank.");
        }

        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("PDF file does not exist: " + filePath);
        }

        if (!file.isFile()) {
            throw new IOException("Path does not point to a file: " + filePath);
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            return textStripper.getText(document);
        }
    }
}