package ro.stancalau.test.framework.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileUtilsTest {

    @Test
    void sanitizeFileName_withNull_returnsEmptyString() {
        assertEquals("", FileUtils.sanitizeFileName(null));
    }

    @Test
    void sanitizeFileName_withEmptyString_returnsEmptyString() {
        assertEquals("", FileUtils.sanitizeFileName(""));
    }

    @Test
    void sanitizeFileName_withValidName_returnsUnchanged() {
        assertEquals("validfilename", FileUtils.sanitizeFileName("validfilename"));
        assertEquals("valid-file_name", FileUtils.sanitizeFileName("valid-file_name"));
        assertEquals("ValidFile123", FileUtils.sanitizeFileName("ValidFile123"));
    }

    @Test
    void sanitizeFileName_withSpaces_convertsToUnderscores() {
        assertEquals("file_name", FileUtils.sanitizeFileName("file name"));
        assertEquals("multiple_spaces_here", FileUtils.sanitizeFileName("multiple   spaces    here"));
        assertEquals("leading_and_trailing", FileUtils.sanitizeFileName(" leading and trailing "));
    }

    @Test
    void sanitizeFileName_withSpecialCharacters_removesInvalidChars() {
        assertEquals("file_name", FileUtils.sanitizeFileName("file@name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file#name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file$name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file%name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file&name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file*name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file(name)"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file[name]"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file{name}"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file|name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file\\name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file/name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file:name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file;name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file\"name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file'name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file<name>"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file?name"));
    }

    @Test
    void sanitizeFileName_withMultipleUnderscores_consolidates() {
        assertEquals("file_name", FileUtils.sanitizeFileName("file___name"));
        assertEquals("file_name", FileUtils.sanitizeFileName("file_____name"));
        assertEquals("test", FileUtils.sanitizeFileName("____test____"));
    }

    @Test
    void sanitizeFileName_withLeadingTrailingUnderscores_removes() {
        assertEquals("filename", FileUtils.sanitizeFileName("_filename_"));
        assertEquals("test", FileUtils.sanitizeFileName("___test___"));
    }

    @Test
    void sanitizeFileName_realWorldExamples_workCorrectly() {
        assertEquals("Record_video_from_publisher", FileUtils.sanitizeFileName("Record video from publisher"));
        assertEquals("LiveKit_WebRTC_Publish", FileUtils.sanitizeFileName("LiveKit WebRTC Publish"));
        assertEquals("Test_with_special_chars_123", FileUtils.sanitizeFileName("Test with @special #chars! 123"));
        assertEquals("", FileUtils.sanitizeFileName("@#$%^&*()"));
        assertEquals(
                "multiple_spaces_normalized", FileUtils.sanitizeFileName("   multiple    spaces     normalized   "));
    }

    @Test
    void sanitizeFileNameStrict_withNull_returnsEmptyString() {
        assertEquals("", FileUtils.sanitizeFileNameStrict(null));
    }

    @Test
    void sanitizeFileNameStrict_withSpaces_convertsToUnderscores() {
        assertEquals("file_name", FileUtils.sanitizeFileNameStrict("file name"));
        assertEquals("multiple_spaces", FileUtils.sanitizeFileNameStrict("multiple   spaces"));
    }

    @Test
    void sanitizeFileNameStrict_withValidChars_preserves() {
        assertEquals("valid_file.name-123", FileUtils.sanitizeFileNameStrict("valid_file.name-123"));
        assertEquals("test.txt", FileUtils.sanitizeFileNameStrict("test.txt"));
    }

    @Test
    void sanitizeFileNameStrict_withInvalidChars_convertsToUnderscores() {
        assertEquals("file_name_test", FileUtils.sanitizeFileNameStrict("file@name#test"));
        assertEquals("test_file", FileUtils.sanitizeFileNameStrict("test$file%"));
    }

    @Test
    void sanitizeFileNameStrict_consolidatesUnderscores() {
        assertEquals("file_name", FileUtils.sanitizeFileNameStrict("file@@@name"));
        assertEquals("test_example", FileUtils.sanitizeFileNameStrict("test   example"));
    }

    @Test
    void sanitizePath_withNull_returnsEmptyString() {
        assertEquals("", FileUtils.sanitizePath(null));
    }

    @Test
    void sanitizePath_withEmptyString_returnsEmptyString() {
        assertEquals("", FileUtils.sanitizePath(""));
    }

    @Test
    void sanitizePath_withValidPath_preservesStructure() {
        assertEquals("path/to/file", FileUtils.sanitizePath("path/to/file"));
        assertEquals("valid/path/structure", FileUtils.sanitizePath("valid/path/structure"));
    }

    @Test
    void sanitizePath_withSpacesInSegments_sanitizesSegments() {
        assertEquals("path_with_spaces/to/file_name", FileUtils.sanitizePath("path with spaces/to/file name"));
        assertEquals(
                "multiple_segments/with_spaces/here", FileUtils.sanitizePath("multiple segments/with spaces/here"));
    }

    @Test
    void sanitizePath_withInvalidCharsInSegments_sanitizesSegments() {
        assertEquals("path_to/file_name", FileUtils.sanitizePath("path@to/file#name"));
        assertEquals("test_folder/sub_dir/file", FileUtils.sanitizePath("test@folder/sub#dir/file"));
    }

    @Test
    void sanitizePath_withBackslashes_convertsToForwardSlashes() {
        assertEquals("windows/style/path", FileUtils.sanitizePath("windows\\style\\path"));
        assertEquals("mixed/slash/path", FileUtils.sanitizePath("mixed\\slash/path"));
    }

    @Test
    void sanitizePath_realWorldExamples_workCorrectly() {
        assertEquals(
                "out/bdd/scenarios/LiveKit_Test/Test_Scenario",
                FileUtils.sanitizePath("out/bdd/scenarios/LiveKit Test/Test Scenario"));
        assertEquals("feature/name/with_special_chars", FileUtils.sanitizePath("feature\\name\\with@special#chars"));
        assertEquals(
                "deep/nested/path/with_multiple_issues",
                FileUtils.sanitizePath("deep/nested/path/with   multiple@issues"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"simple", "with-hyphens", "with_underscores", "WithCamelCase", "with123numbers"})
    void sanitizeFileName_withValidNames_remainsUnchanged(String validName) {
        assertEquals(validName, FileUtils.sanitizeFileName(validName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"file@name", "file#name", "file$name", "file%name", "file&name", "file*name"})
    void sanitizeFileName_withInvalidChars_replacesWithUnderscore(String invalidName) {
        String result = FileUtils.sanitizeFileName(invalidName);
        assertEquals("file_name", result);
        assertFalse(result.matches(".*[^a-zA-Z0-9\\-_].*"), "Result should only contain valid characters");
    }
}
