package extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

public final class WordExtractor {

    private WordExtractor() {
    }

    public static String extractText(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Word file path cannot be blank.");
        }

        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("Word file does not exist: " + filePath);
        }

        if (!file.isFile()) {
            throw new IOException("Path does not point to a file: " + filePath);
        }

        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".docx")) {
            return extractDocx(file);
        }

        if (fileName.endsWith(".doc")) {
            return extractDoc(file);
        }

        throw new IllegalArgumentException(
                "Unsupported Word file format. Only .doc and .docx are supported."
        );
    }

    private static String extractDocx(File file) throws IOException {
        StringBuilder text = new StringBuilder();

        try (FileInputStream inputStream = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(inputStream)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText());
                text.append(System.lineSeparator());
            }

            for (XWPFTable table : document.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        text.append(cell.getText());
                        text.append(" ");
                    }

                    text.append(System.lineSeparator());
                }
            }
        }

        return text.toString();
    }

    private static String extractDoc(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(inputStream)) {

            return document.getDocumentText();
        }
    }
}
