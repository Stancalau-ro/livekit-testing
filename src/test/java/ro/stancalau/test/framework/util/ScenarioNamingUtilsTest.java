package ro.stancalau.test.framework.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScenarioNamingUtilsTest {

    @ParameterizedTest
    @CsvSource({
        "file:///path/to/test_feature.feature, Test Feature",
        "file:///path/to/another_test_scenario.feature, Another Test Scenario",
        "file:///path/to/simple.feature, Simple",
        "file:///path/to/multi_word_feature_name.feature, Multi Word Feature Name",
        "classpath:features/livekit_access_token.feature, Livekit Access Token",
        "classpath:features/livekit_webrtc_publish.feature, Livekit Webrtc Publish",
        "file:///home/user/project/src/test/resources/features/my_awesome_test.feature, My Awesome Test"
    })
    void extractFeatureName_shouldExtractAndFormatFeatureNameCorrectly(String uri, String expected) {
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals(expected, result);
    }

    @Test
    void extractFeatureName_shouldHandleFileWithoutFeatureExtension() {
        String uri = "file:///path/to/test_file";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test File", result);
    }

    @Test
    void extractFeatureName_shouldHandleEmptyFileName() {
        String uri = "file:///path/to/";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("", result);
    }

    @Test
    void extractFeatureName_shouldHandleMultipleUnderscores() {
        String uri = "file:///path/to/test___multiple___underscores.feature";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test Multiple Underscores", result);
    }

    @Test
    void extractFeatureName_shouldHandleMixedCaseFileName() {
        String uri = "file:///path/to/Test_MIXED_case.feature";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test Mixed Case", result);
    }

    @Test
    void extractFeatureName_shouldHandleFileNameWithNumbers() {
        String uri = "file:///path/to/test_123_feature.feature";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test 123 Feature", result);
    }

    @Test
    void extractFeatureName_shouldHandleInvalidUri() {
        String uri = "invalid uri without slashes";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Invalid Uri Without Slashes", result);
    }

    @Test
    void extractFeatureName_shouldReturnUnknownFeatureForNullUri() {
        String result = ScenarioNamingUtils.extractFeatureName(null);
        assertEquals("Unknown Feature", result);
    }

    @Test
    void extractFeatureName_shouldHandleWindowsStylePath() {
        String uri = "file:///C:/Users/test/project/test_windows_path.feature";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test Windows Path", result);
    }

    @Test
    void extractFeatureName_shouldHandleFeatureFileWithDots() {
        String uri = "file:///path/to/test.with.dots.feature";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test.with.dots", result);
    }

    @Test
    void extractFeatureName_shouldHandleFeatureFileWithHyphens() {
        String uri = "file:///path/to/test-with-hyphens.feature";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test-with-hyphens", result);
    }

    @Test
    void extractFeatureName_shouldHandleSingleWordFileName() {
        String uri = "file:///path/to/test.feature";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test", result);
    }

    @Test
    void extractFeatureName_shouldPreserveSpacesInFileName() {
        String uri = "file:///path/to/test with spaces.feature";
        String result = ScenarioNamingUtils.extractFeatureName(uri);
        assertEquals("Test With Spaces", result);
    }
}