package ro.stancalau.test.framework.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ScenarioNamingUtils {

  public static String extractFeatureName(String uri) {
    try {
      String fileName = uri.substring(uri.lastIndexOf('/') + 1);
      if (fileName.endsWith(".feature")) {
        fileName = fileName.substring(0, fileName.length() - 8);
      }
      String[] words = fileName.replace('_', ' ').toLowerCase().split(" ");
      StringBuilder result = new StringBuilder();
      for (String word : words) {
        if (!word.isEmpty()) {
          if (!result.isEmpty()) {
            result.append(" ");
          }
          result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
      }
      return result.toString();
    } catch (Exception e) {
      log.warn("Failed to extract feature name from URI: {}", uri, e);
      return "Unknown Feature";
    }
  }
}
