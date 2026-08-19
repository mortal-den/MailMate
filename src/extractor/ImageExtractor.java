package extractor;

import java.io.File;
import java.io.IOException;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public final class ImageExtractor {

    private ImageExtractor() {
    }

    public static String extractText(String filePath) throws IOException {

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Image file path cannot be blank."
            );
        }

        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException(
                    "Image file does not exist: " + filePath
            );
        }

        if (!file.isFile()) {
            throw new IOException(
                    "Path does not point to a file: " + filePath
            );
        }

        String fileName = file.getName().toLowerCase();

        if (!isSupportedImage(fileName)) {
            throw new IllegalArgumentException(
                    "Unsupported image format. Supported formats: "
                    + "PNG, JPG, JPEG, BMP, GIF and TIFF."
            );
        }

        Tesseract tesseract = new Tesseract();

        try {
            return tesseract.doOCR(file);
        } catch (TesseractException e) {
            throw new IOException(
                    "Failed to extract text from image: " + filePath,
                    e
            );
        }
    }

    private static boolean isSupportedImage(String fileName) {

        return fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".bmp")
                || fileName.endsWith(".gif")
                || fileName.endsWith(".tif")
                || fileName.endsWith(".tiff");
    }
}
