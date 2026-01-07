package ro.stancalau.test.framework.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void scenarioTimestampFormat_hasCorrectPattern() {
        // Test by formatting a known date to verify the pattern
        LocalDateTime testDateTime = LocalDateTime.of(2025, 8, 6, 13, 25, 59);
        String formatted = DateUtils.SCENARIO_TIMESTAMP_FORMAT.format(testDateTime);
        assertEquals("2025-08-06_13-25-59", formatted);
    }

    @Test
    void recordingTimestampFormat_hasCorrectPattern() {
        // Test by formatting a known date to verify the pattern
        LocalDateTime testDateTime = LocalDateTime.of(2025, 8, 6, 13, 25, 59);
        String formatted = DateUtils.RECORDING_TIMESTAMP_FORMAT.format(testDateTime);
        assertEquals("2025-08-06T132559", formatted);
    }

    @Test
    void generateScenarioTimestamp_returnsValidFormat() {
        String timestamp = DateUtils.generateScenarioTimestamp();

        // Should match pattern: yyyy-MM-dd_HH-mm-ss
        Pattern expectedPattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}");
        assertTrue(
                expectedPattern.matcher(timestamp).matches(),
                "Generated timestamp '" + timestamp + "' should match pattern yyyy-MM-dd_HH-mm-ss");

        // Should be 19 characters long
        assertEquals(19, timestamp.length());
    }

    @Test
    void generateRecordingTimestamp_returnsValidFormat() {
        String timestamp = DateUtils.generateRecordingTimestamp();

        // Should match pattern: yyyy-MM-dd'T'HHmmss
        Pattern expectedPattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{6}");
        assertTrue(
                expectedPattern.matcher(timestamp).matches(),
                "Generated timestamp '" + timestamp + "' should match pattern yyyy-MM-dd'T'HHmmss");

        // Should be 17 characters long
        assertEquals(17, timestamp.length());
    }

    @Test
    void formatScenarioTimestamp_withSpecificDateTime_returnsExpectedFormat() {
        LocalDateTime testDateTime = LocalDateTime.of(2025, 8, 6, 13, 25, 59);
        String formatted = DateUtils.formatScenarioTimestamp(testDateTime);

        assertEquals("2025-08-06_13-25-59", formatted);
    }

    @Test
    void formatRecordingTimestamp_withSpecificDateTime_returnsExpectedFormat() {
        LocalDateTime testDateTime = LocalDateTime.of(2025, 8, 6, 13, 25, 59);
        String formatted = DateUtils.formatRecordingTimestamp(testDateTime);

        assertEquals("2025-08-06T132559", formatted);
    }

    @Test
    void generateScenarioTimestamp_multipleCallsWithinSecond_returnsSameValue() {
        String timestamp1 = DateUtils.generateScenarioTimestamp();
        String timestamp2 = DateUtils.generateScenarioTimestamp();

        // Since both calls happen within the same second, they should be identical
        assertEquals(timestamp1, timestamp2);
    }

    @Test
    void generateRecordingTimestamp_multipleCallsWithinSecond_returnsSameValue() {
        String timestamp1 = DateUtils.generateRecordingTimestamp();
        String timestamp2 = DateUtils.generateRecordingTimestamp();

        // Since both calls happen within the same second, they should be identical
        assertEquals(timestamp1, timestamp2);
    }

    @Test
    void scenarioTimestamp_isFileSystemSafe() {
        String timestamp = DateUtils.generateScenarioTimestamp();

        // Should not contain any problematic characters for file systems
        assertFalse(timestamp.contains("/"));
        assertFalse(timestamp.contains("\\"));
        assertFalse(timestamp.contains(":"));
        assertFalse(timestamp.contains("*"));
        assertFalse(timestamp.contains("?"));
        assertFalse(timestamp.contains("\""));
        assertFalse(timestamp.contains("<"));
        assertFalse(timestamp.contains(">"));
        assertFalse(timestamp.contains("|"));
    }

    @Test
    void recordingTimestamp_isFileSystemSafe() {
        String timestamp = DateUtils.generateRecordingTimestamp();

        // Should not contain any problematic characters for file systems
        assertFalse(timestamp.contains("/"));
        assertFalse(timestamp.contains("\\"));
        assertFalse(timestamp.contains(":")); // T is used instead of :
        assertFalse(timestamp.contains("*"));
        assertFalse(timestamp.contains("?"));
        assertFalse(timestamp.contains("\""));
        assertFalse(timestamp.contains("<"));
        assertFalse(timestamp.contains(">"));
        assertFalse(timestamp.contains("|"));
    }

    @Test
    void scenarioTimestamp_containsExpectedSeparators() {
        String timestamp = DateUtils.generateScenarioTimestamp();

        assertTrue(timestamp.contains("-")); // Date separators
        assertTrue(timestamp.contains("_")); // Date-time separator
    }

    @Test
    void recordingTimestamp_containsExpectedSeparators() {
        String timestamp = DateUtils.generateRecordingTimestamp();

        assertTrue(timestamp.contains("-")); // Date separators
        assertTrue(timestamp.contains("T")); // Date-time separator (ISO-like)
    }

    @Test
    void formatters_handleEdgeCases() {
        // Test with edge cases like single digits
        LocalDateTime edgeCase = LocalDateTime.of(2025, 1, 1, 1, 1, 1);

        String scenarioTimestamp = DateUtils.formatScenarioTimestamp(edgeCase);
        assertEquals("2025-01-01_01-01-01", scenarioTimestamp);

        String recordingTimestamp = DateUtils.formatRecordingTimestamp(edgeCase);
        assertEquals("2025-01-01T010101", recordingTimestamp);
    }

    @Test
    void formatters_handleEndOfYear() {
        LocalDateTime endOfYear = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        String scenarioTimestamp = DateUtils.formatScenarioTimestamp(endOfYear);
        assertEquals("2025-12-31_23-59-59", scenarioTimestamp);

        String recordingTimestamp = DateUtils.formatRecordingTimestamp(endOfYear);
        assertEquals("2025-12-31T235959", recordingTimestamp);
    }
}
