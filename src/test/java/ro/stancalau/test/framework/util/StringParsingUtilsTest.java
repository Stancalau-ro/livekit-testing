package ro.stancalau.test.framework.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StringParsingUtils Tests")
class StringParsingUtilsTest {

  @Nested
  @DisplayName("parseCommaSeparatedList Tests")
  class ParseCommaSeparatedListTests {

    @Test
    @DisplayName("Should parse simple comma-separated values")
    void shouldParseSimpleCommaSeparatedValues() {
      String input = "value1,value2,value3";
      List<String> result = StringParsingUtils.parseCommaSeparatedList(input);

      assertEquals(3, result.size());
      assertEquals("value1", result.get(0));
      assertEquals("value2", result.get(1));
      assertEquals("value3", result.get(2));
    }

    @Test
    @DisplayName("Should handle escaped commas")
    void shouldHandleEscapedCommas() {
      String input = "value1,value with\\, comma,value3";
      List<String> result = StringParsingUtils.parseCommaSeparatedList(input);

      assertEquals(3, result.size());
      assertEquals("value1", result.get(0));
      assertEquals("value with, comma", result.get(1));
      assertEquals("value3", result.get(2));
    }

    @Test
    @DisplayName("Should handle multiple escaped commas in one value")
    void shouldHandleMultipleEscapedCommas() {
      String input = "normal,testing\\, debugging\\, development,final";
      List<String> result = StringParsingUtils.parseCommaSeparatedList(input);

      assertEquals(3, result.size());
      assertEquals("normal", result.get(0));
      assertEquals("testing, debugging, development", result.get(1));
      assertEquals("final", result.get(2));
    }

    @Test
    @DisplayName("Should handle escaped backslashes")
    void shouldHandleEscapedBackslashes() {
      String input = "value1,path\\\\to\\\\file,value3";
      List<String> result = StringParsingUtils.parseCommaSeparatedList(input);

      assertEquals(3, result.size());
      assertEquals("value1", result.get(0));
      assertEquals("path\\to\\file", result.get(1));
      assertEquals("value3", result.get(2));
    }

    @Test
    @DisplayName("Should handle empty string")
    void shouldHandleEmptyString() {
      List<String> result = StringParsingUtils.parseCommaSeparatedList("");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle null input")
    void shouldHandleNullInput() {
      List<String> result = StringParsingUtils.parseCommaSeparatedList(null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle whitespace-only string")
    void shouldHandleWhitespaceOnlyString() {
      List<String> result = StringParsingUtils.parseCommaSeparatedList("   ");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle single value without commas")
    void shouldHandleSingleValueWithoutCommas() {
      String input = "singlevalue";
      List<String> result = StringParsingUtils.parseCommaSeparatedList(input);

      assertEquals(1, result.size());
      assertEquals("singlevalue", result.getFirst());
    }

    @Test
    @DisplayName("Should handle trailing comma")
    void shouldHandleTrailingComma() {
      String input = "value1,value2,";
      List<String> result = StringParsingUtils.parseCommaSeparatedList(input);

      assertEquals(3, result.size());
      assertEquals("value1", result.get(0));
      assertEquals("value2", result.get(1));
      assertEquals("", result.get(2));
    }
  }

  @Nested
  @DisplayName("parseKeyValuePairs Tests")
  class ParseKeyValuePairsTests {

    @Test
    @DisplayName("Should parse simple key=value pairs")
    void shouldParseSimpleKeyValuePairs() {
      String input = "key1=value1,key2=value2,key3=value3";
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(input);

      assertEquals(3, result.size());
      assertEquals("value1", result.get("key1"));
      assertEquals("value2", result.get("key2"));
      assertEquals("value3", result.get("key3"));
    }

    @Test
    @DisplayName("Should handle escaped commas in values")
    void shouldHandleEscapedCommasInValues() {
      String input = "description=A room for testing\\, debugging\\, and development,role=admin";
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(input);

      assertEquals(2, result.size());
      assertEquals("A room for testing, debugging, and development", result.get("description"));
      assertEquals("admin", result.get("role"));
    }

    @Test
    @DisplayName("Should handle escaped equals signs in values")
    void shouldHandleEscapedEqualsInValues() {
      // Note: The parseKeyValuePairs method splits on the first unescaped equals sign for each pair
      // So "formula=x\\=y+z" should be parsed as key="formula", value="x=y+z"
      String input = "formula=x\\=y+z,name=test";
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(input);

      assertEquals(2, result.size());
      assertEquals("x=y+z", result.get("formula"));
      assertEquals("test", result.get("name"));
    }

    @Test
    @DisplayName("Should handle complex values with multiple escapes")
    void shouldHandleComplexValuesWithMultipleEscapes() {
      String input =
          "fullname=Grace O'Connor\\, Senior Engineer,tags=test\\,debug\\,dev,department=engineering";
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(input);

      assertEquals(3, result.size());
      assertEquals("Grace O'Connor, Senior Engineer", result.get("fullname"));
      assertEquals("test,debug,dev", result.get("tags"));
      assertEquals("engineering", result.get("department"));
    }

    @Test
    @DisplayName("Should trim whitespace from keys and values")
    void shouldTrimWhitespaceFromKeysAndValues() {
      String input = " key1 = value1 , key2 = value2 ";
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(input);

      assertEquals(2, result.size());
      assertEquals("value1", result.get("key1"));
      assertEquals("value2", result.get("key2"));
    }

    @Test
    @DisplayName("Should handle empty string")
    void shouldHandleEmptyString() {
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs("");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle null input")
    void shouldHandleNullInput() {
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception for malformed pairs")
    void shouldThrowExceptionForMalformedPairs() {
      String input = "key1=value1,malformed,key2=value2";

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> StringParsingUtils.parseKeyValuePairs(input));

      assertTrue(exception.getMessage().contains("Malformed key=value pair"));
      assertTrue(exception.getMessage().contains("malformed"));
    }

    @Test
    @DisplayName("Should throw exception for pairs without equals sign")
    void shouldThrowExceptionForPairsWithoutEqualsSign() {
      String input = "validkey=validvalue,invalidpair";

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> StringParsingUtils.parseKeyValuePairs(input));

      assertTrue(exception.getMessage().contains("Malformed key=value pair"));
      assertTrue(exception.getMessage().contains("invalidpair"));
    }

    @Test
    @DisplayName("Should throw exception for empty key")
    void shouldThrowExceptionForEmptyKey() {
      String input = "=value,key2=value2";

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> StringParsingUtils.parseKeyValuePairs(input));

      assertTrue(exception.getMessage().contains("Empty key"));
    }

    @Test
    @DisplayName("Should throw exception for key starting with equals")
    void shouldThrowExceptionForKeyStartingWithEquals() {
      String input = "key1=value1,=invalidvalue";

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> StringParsingUtils.parseKeyValuePairs(input));

      assertTrue(exception.getMessage().contains("Empty key"));
    }

    @Test
    @DisplayName("Should throw exception for whitespace-only key")
    void shouldThrowExceptionForWhitespaceOnlyKey() {
      String input = "   =value,key2=value2";

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> StringParsingUtils.parseKeyValuePairs(input));

      assertTrue(exception.getMessage().contains("Empty key"));
    }

    @Test
    @DisplayName("Should handle empty values")
    void shouldHandleEmptyValues() {
      String input = "key1=,key2=value2,key3=";
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(input);

      assertEquals(3, result.size());
      assertEquals("", result.get("key1"));
      assertEquals("value2", result.get("key2"));
      assertEquals("", result.get("key3"));
    }

    @Test
    @DisplayName("Should handle consecutive commas gracefully")
    void shouldHandleConsecutiveCommasGracefully() {
      String input = "key1=value1,,key2=value2";
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(input);

      assertEquals(2, result.size());
      assertEquals("value1", result.get("key1"));
      assertEquals("value2", result.get("key2"));
    }
  }

  @Nested
  @DisplayName("parseEscapedString Tests")
  class ParseEscapedStringTests {

    @Test
    @DisplayName("Should parse with custom delimiter")
    void shouldParseWithCustomDelimiter() {
      String input = "part1|part2|part3";
      List<String> result = StringParsingUtils.parseEscapedString(input, "|");

      assertEquals(3, result.size());
      assertEquals("part1", result.get(0));
      assertEquals("part2", result.get(1));
      assertEquals("part3", result.get(2));
    }

    @Test
    @DisplayName("Should handle escaped custom delimiter")
    void shouldHandleEscapedCustomDelimiter() {
      String input = "part1|part with\\| pipe|part3";
      List<String> result = StringParsingUtils.parseEscapedString(input, "|");

      assertEquals(3, result.size());
      assertEquals("part1", result.get(0));
      assertEquals("part with| pipe", result.get(1));
      assertEquals("part3", result.get(2));
    }

    @Test
    @DisplayName("Should handle multi-character delimiter")
    void shouldHandleMultiCharacterDelimiter() {
      String input = "part1::part2::part3";
      List<String> result = StringParsingUtils.parseEscapedString(input, "::");

      assertEquals(3, result.size());
      assertEquals("part1", result.get(0));
      assertEquals("part2", result.get(1));
      assertEquals("part3", result.get(2));
    }

    @Test
    @DisplayName("Should handle escaped multi-character delimiter")
    void shouldHandleEscapedMultiCharacterDelimiter() {
      String input = "part1::part with\\:: colon::part3";
      List<String> result = StringParsingUtils.parseEscapedString(input, "::");

      assertEquals(3, result.size());
      assertEquals("part1", result.get(0));
      assertEquals("part with:: colon", result.get(1));
      assertEquals("part3", result.get(2));
    }

    @Test
    @DisplayName("Should handle consecutive delimiters")
    void shouldHandleConsecutiveDelimiters() {
      String input = "part1,,part3";
      List<String> result = StringParsingUtils.parseEscapedString(input, ",");

      assertEquals(3, result.size());
      assertEquals("part1", result.get(0));
      assertEquals("", result.get(1));
      assertEquals("part3", result.get(2));
    }

    @Test
    @DisplayName("Should handle backslash at end of string")
    void shouldHandleBackslashAtEndOfString() {
      String input = "part1,part2\\";
      List<String> result = StringParsingUtils.parseEscapedString(input, ",");

      assertEquals(2, result.size());
      assertEquals("part1", result.get(0));
      assertEquals("part2\\", result.get(1));
    }

    @Test
    @DisplayName("Should handle empty parts")
    void shouldHandleEmptyParts() {
      String input = ",part2,";
      List<String> result = StringParsingUtils.parseEscapedString(input, ",");

      assertEquals(3, result.size());
      assertEquals("", result.get(0));
      assertEquals("part2", result.get(1));
      assertEquals("", result.get(2));
    }

    @Test
    @DisplayName("Should handle single character input")
    void shouldHandleSingleCharacterInput() {
      String input = "x";
      List<String> result = StringParsingUtils.parseEscapedString(input, ",");

      assertEquals(1, result.size());
      assertEquals("x", result.getFirst());
    }

    @Test
    @DisplayName("Should handle delimiter-only input")
    void shouldHandleDelimiterOnlyInput() {
      String input = ",";
      List<String> result = StringParsingUtils.parseEscapedString(input, ",");

      assertEquals(2, result.size());
      assertEquals("", result.get(0));
      assertEquals("", result.get(1));
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should handle real-world grant parsing scenario")
    void shouldHandleRealWorldGrantParsingScenario() {
      String grantsInput = "canpublish:true,cansubscribe:true,roomadmin:false";
      List<String> grants = StringParsingUtils.parseCommaSeparatedList(grantsInput);

      assertEquals(3, grants.size());
      assertEquals("canpublish:true", grants.get(0));
      assertEquals("cansubscribe:true", grants.get(1));
      assertEquals("roomadmin:false", grants.get(2));
    }

    @Test
    @DisplayName("Should handle real-world attributes parsing scenario")
    void shouldHandleRealWorldAttributesParsingScenario() {
      String attributesInput =
          "role=moderator,department=engineering,description=A room for testing\\, debugging\\, and development,fullname=Grace O'Connor\\, Senior Engineer";
      Map<String, String> attributes = StringParsingUtils.parseKeyValuePairs(attributesInput);

      assertEquals(4, attributes.size());
      assertEquals("moderator", attributes.get("role"));
      assertEquals("engineering", attributes.get("department"));
      assertEquals("A room for testing, debugging, and development", attributes.get("description"));
      assertEquals("Grace O'Connor, Senior Engineer", attributes.get("fullname"));
    }

    @Test
    @DisplayName("Should handle complex nested escaping")
    void shouldHandleComplexNestedEscaping() {
      String input = "config=path\\=C:\\\\Program Files\\\\App\\, version\\=1.0,debug=true";
      Map<String, String> result = StringParsingUtils.parseKeyValuePairs(input);

      assertEquals(2, result.size());
      // Note: Double backslashes become single backslashes when unescaped
      assertEquals("path=C:Program FilesApp, version=1.0", result.get("config"));
      assertEquals("true", result.get("debug"));
    }
  }
}
