package ro.stancalau.test.framework.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageValidationUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void testIsValidImage_ValidJpegImage() throws IOException {
        File imageFile = createTestJpegImage(tempDir, "valid.jpg", 100, 100);
        
        assertTrue(ImageValidationUtils.isValidImage(imageFile));
    }

    @Test
    void testIsValidImage_NonExistentFile() {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.jpg");
        
        assertFalse(ImageValidationUtils.isValidImage(nonExistentFile));
    }

    @Test
    void testIsValidImage_EmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.jpg").toFile();
        emptyFile.createNewFile();
        
        assertFalse(ImageValidationUtils.isValidImage(emptyFile));
    }

    @Test
    void testIsValidImage_NotAnImageFile() throws IOException {
        File textFile = tempDir.resolve("text.jpg").toFile();
        Files.write(textFile.toPath(), "This is not an image".getBytes());
        
        assertFalse(ImageValidationUtils.isValidImage(textFile));
    }

    @Test
    void testHasMinimumSize_ValidSize() throws IOException {
        File imageFile = createTestJpegImage(tempDir, "large.jpg", 200, 200);
        long fileSize = imageFile.length();
        
        assertTrue(ImageValidationUtils.hasMinimumSize(imageFile, fileSize - 100));
        assertFalse(ImageValidationUtils.hasMinimumSize(imageFile, fileSize + 100));
    }

    @Test
    void testHasMinimumSize_NonExistentFile() {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.jpg");
        
        assertFalse(ImageValidationUtils.hasMinimumSize(nonExistentFile, 1000));
    }

    @Test
    void testHasValidDimensions_ValidDimensions() throws IOException {
        File imageFile = createTestJpegImage(tempDir, "dimensions.jpg", 300, 200);
        
        assertTrue(ImageValidationUtils.hasValidDimensions(imageFile, 250, 150));
        assertTrue(ImageValidationUtils.hasValidDimensions(imageFile, 300, 200));
        assertFalse(ImageValidationUtils.hasValidDimensions(imageFile, 350, 150));
        assertFalse(ImageValidationUtils.hasValidDimensions(imageFile, 250, 250));
    }

    @Test
    void testHasValidDimensions_NonExistentFile() {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.jpg");
        
        assertFalse(ImageValidationUtils.hasValidDimensions(nonExistentFile, 100, 100));
    }

    @Test
    void testHasValidDimensions_InvalidImageFile() throws IOException {
        File textFile = tempDir.resolve("invalid.jpg").toFile();
        Files.write(textFile.toPath(), "Not an image".getBytes());
        
        assertFalse(ImageValidationUtils.hasValidDimensions(textFile, 100, 100));
    }

    @Test
    void testIsValidImage_ZeroDimensions() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        File imageFile = tempDir.resolve("tiny_dimensions.jpg").toFile();
        ImageIO.write(image, "jpg", imageFile);
        
        assertTrue(ImageValidationUtils.isValidImage(imageFile));
        
        assertFalse(ImageValidationUtils.hasValidDimensions(imageFile, 10, 10));
    }

    private File createTestJpegImage(Path tempDir, String filename, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, width / 2, height);
        
        g2d.setColor(Color.BLUE);
        g2d.fillRect(width / 2, 0, width / 2, height);
        
        g2d.dispose();
        
        File imageFile = tempDir.resolve(filename).toFile();
        ImageIO.write(image, "jpg", imageFile);
        
        return imageFile;
    }
}