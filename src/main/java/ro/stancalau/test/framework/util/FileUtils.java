package ro.stancalau.test.framework.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class FileUtils {

  /**
   * Sanitizes a filename by removing or replacing invalid characters for filesystem compatibility.
   * This method preserves spaces and converts them to underscores, while removing other invalid
   * characters.
   *
   * @param fileName the filename to sanitize
   * @return a sanitized filename safe for use across different filesystems
   */
  public static String sanitizeFileName(String fileName) {
    if (fileName == null) {
      return "";
    }

    return fileName
        .replaceAll("[^a-zA-Z0-9\\-_\\s]", "_") // Convert invalid chars to underscores, keep spaces
        .replaceAll("\\s+", "_") // Convert multiple spaces to single underscore
        .replaceAll("_+", "_") // Convert multiple underscores to single underscore
        .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
  }

  /**
   * Sanitizes a filename by converting all invalid characters (including spaces) to underscores.
   * This is more restrictive than {@link #sanitizeFileName(String)} and ensures no spaces remain.
   *
   * @param fileName the filename to sanitize
   * @return a sanitized filename with all invalid characters converted to underscores
   */
  public static String sanitizeFileNameStrict(String fileName) {
    if (fileName == null) {
      return "";
    }

    return fileName
        .replaceAll(
            "[^a-zA-Z0-9._-]", "_") // Convert invalid chars (including spaces) to underscores
        .replaceAll("_+", "_") // Convert multiple underscores to single underscore
        .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
  }

  public static String sanitizePath(String path) {
    if (path == null) {
      return "";
    }

    // Split on both forward and backward slashes, sanitize each segment, then rejoin
    String[] segments = path.split("[/\\\\]");
    if (segments.length == 0) {
      return "";
    }

    StringBuilder sanitizedPath = new StringBuilder();
    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        sanitizedPath.append("/"); // Use forward slash for consistent output
      }
      sanitizedPath.append(sanitizeFileName(segments[i]));
    }

    return sanitizedPath.toString();
  }
}
