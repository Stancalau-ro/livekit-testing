package ro.stancalau.test.framework.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateUtils {

    /**
     * Formatter for scenario timestamps used in log directory names. Pattern: yyyy-MM-dd_HH-mm-ss
     * Example: 2025-08-06_13-25-59
     */
    public static final DateTimeFormatter SCENARIO_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Formatter for recording file timestamps used in recording filenames. Pattern:
     * yyyy-MM-dd'T'HHmmss (ISO-like format without separators) Example: 2025-08-06T132559
     */
    public static final DateTimeFormatter RECORDING_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss");

    /**
     * Generates a scenario timestamp string using the current date and time. Used for creating
     * scenario-specific log directories.
     *
     * @return formatted timestamp string suitable for directory names
     */
    public static String generateScenarioTimestamp() {
        return LocalDateTime.now().format(SCENARIO_TIMESTAMP_FORMAT);
    }

    /**
     * Generates a recording timestamp string using the current date and time. Used for creating
     * unique recording filenames.
     *
     * @return formatted timestamp string suitable for filenames
     */
    public static String generateRecordingTimestamp() {
        return LocalDateTime.now().format(RECORDING_TIMESTAMP_FORMAT);
    }

    /**
     * Formats a given LocalDateTime using the scenario timestamp format.
     *
     * @param dateTime the date time to format
     * @return formatted timestamp string suitable for directory names
     */
    public static String formatScenarioTimestamp(LocalDateTime dateTime) {
        return dateTime.format(SCENARIO_TIMESTAMP_FORMAT);
    }

    /**
     * Formats a given LocalDateTime using the recording timestamp format.
     *
     * @param dateTime the date time to format
     * @return formatted timestamp string suitable for filenames
     */
    public static String formatRecordingTimestamp(LocalDateTime dateTime) {
        return dateTime.format(RECORDING_TIMESTAMP_FORMAT);
    }
}
