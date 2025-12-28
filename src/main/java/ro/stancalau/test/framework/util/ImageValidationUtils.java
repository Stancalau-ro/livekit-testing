package ro.stancalau.test.framework.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageValidationUtils {

  public static boolean isValidImage(File imageFile) {
    if (!imageFile.exists()) {
      log.error("Image file does not exist: {}", imageFile.getAbsolutePath());
      return false;
    }

    if (imageFile.length() == 0) {
      log.error("Image file is empty: {}", imageFile.getAbsolutePath());
      return false;
    }

    try {
      BufferedImage image = ImageIO.read(imageFile);
      if (image == null) {
        log.error("Cannot read image file as valid image: {}", imageFile.getAbsolutePath());
        return false;
      }

      if (image.getWidth() <= 0 || image.getHeight() <= 0) {
        log.error(
            "Image has invalid dimensions: {}x{} for file: {}",
            image.getWidth(),
            image.getHeight(),
            imageFile.getAbsolutePath());
        return false;
      }

      log.debug(
          "Valid image found: {} ({}x{}, {} bytes)",
          imageFile.getName(),
          image.getWidth(),
          image.getHeight(),
          imageFile.length());
      return true;

    } catch (IOException e) {
      log.error("Failed to read image file: {}", imageFile.getAbsolutePath(), e);
      return false;
    }
  }

  public static boolean hasMinimumSize(File imageFile, long minimumBytes) {
    if (!imageFile.exists()) {
      return false;
    }

    long fileSize = imageFile.length();
    boolean hasMinSize = fileSize >= minimumBytes;

    if (!hasMinSize) {
      log.warn(
          "Image file {} is smaller than minimum size: {} bytes (minimum: {} bytes)",
          imageFile.getName(),
          fileSize,
          minimumBytes);
    }

    return hasMinSize;
  }

  public static boolean hasValidDimensions(File imageFile, int minWidth, int minHeight) {
    if (!imageFile.exists()) {
      return false;
    }

    try {
      BufferedImage image = ImageIO.read(imageFile);
      if (image == null) {
        return false;
      }

      boolean validWidth = image.getWidth() >= minWidth;
      boolean validHeight = image.getHeight() >= minHeight;

      if (!validWidth || !validHeight) {
        log.warn(
            "Image {} dimensions {}x{} are smaller than minimum {}x{}",
            imageFile.getName(),
            image.getWidth(),
            image.getHeight(),
            minWidth,
            minHeight);
      }

      return validWidth && validHeight;

    } catch (IOException e) {
      log.error("Failed to read image dimensions: {}", imageFile.getAbsolutePath(), e);
      return false;
    }
  }
}
