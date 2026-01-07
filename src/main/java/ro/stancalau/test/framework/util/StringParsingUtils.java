package ro.stancalau.test.framework.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StringParsingUtils {

    private static final Pattern COMPARISON_PATTERN = Pattern.compile("^(>=|<=|>|<)?(\\d+)$");

    @Getter
    @AllArgsConstructor
    public enum ComparisonOperator {
        EQUAL("="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_THAN_OR_EQUAL(">="),
        LESS_THAN_OR_EQUAL("<=");

        private final String symbol;

        public boolean evaluate(int actual, int expected) {
            return switch (this) {
                case EQUAL -> actual == expected;
                case GREATER_THAN -> actual > expected;
                case LESS_THAN -> actual < expected;
                case GREATER_THAN_OR_EQUAL -> actual >= expected;
                case LESS_THAN_OR_EQUAL -> actual <= expected;
            };
        }

        public String formatMessage(String subject, int expected, int actual) {
            return switch (this) {
                case EQUAL -> subject + " should be exactly " + expected + ", found: " + actual;
                case GREATER_THAN -> subject + " should be greater than " + expected + ", found: " + actual;
                case LESS_THAN -> subject + " should be less than " + expected + ", found: " + actual;
                case GREATER_THAN_OR_EQUAL -> subject + " should be at least " + expected + ", found: " + actual;
                case LESS_THAN_OR_EQUAL -> subject + " should be at most " + expected + ", found: " + actual;
            };
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ComparisonExpression {
        private final ComparisonOperator operator;
        private final int value;

        public boolean evaluate(int actual) {
            return operator.evaluate(actual, value);
        }

        public String formatMessage(String subject, int actual) {
            return operator.formatMessage(subject, value, actual);
        }
    }

    public ComparisonExpression parseComparisonExpression(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Comparison expression cannot be null or empty");
        }

        String trimmed = input.trim();
        Matcher matcher = COMPARISON_PATTERN.matcher(trimmed);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid comparison expression: '"
                    + input
                    + "'. Expected format: [>=|<=|>|<]<number> (e.g., '3', '>=2', '<5')");
        }

        String operatorStr = matcher.group(1);
        int value = Integer.parseInt(matcher.group(2));

        ComparisonOperator operator;
        if (operatorStr == null || operatorStr.isEmpty()) {
            operator = ComparisonOperator.EQUAL;
        } else {
            operator = switch (operatorStr) {
                case ">=" -> ComparisonOperator.GREATER_THAN_OR_EQUAL;
                case "<=" -> ComparisonOperator.LESS_THAN_OR_EQUAL;
                case ">" -> ComparisonOperator.GREATER_THAN;
                case "<" -> ComparisonOperator.LESS_THAN;
                default -> throw new IllegalArgumentException("Unknown operator: " + operatorStr);};
        }

        return new ComparisonExpression(operator, value);
    }

    public List<String> parseCommaSeparatedList(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return parseEscapedString(input, ",");
    }

    /**
     * Parses key=value pairs separated by commas with support for escaped characters. Example:
     * "key1=value1,key2=value with\, comma" -> {key1: "value1", key2: "value with, comma"}
     *
     * @param input The input string to parse
     * @return Map of key-value pairs
     * @throws IllegalArgumentException if the input contains malformed key=value pairs
     */
    public Map<String, String> parseKeyValuePairs(String input) {
        Map<String, String> result = new HashMap<>();
        if (input == null || input.trim().isEmpty()) {
            return result;
        }

        List<String> pairs = parseEscapedString(input, ",");
        for (String pair : pairs) {
            String trimmedPair = pair.trim();

            if (trimmedPair.isEmpty()) {
                continue;
            }

            int equalsIndex = findFirstUnescapedChar(trimmedPair, '=');

            if (equalsIndex < 0) {
                throw new IllegalArgumentException(
                        "Malformed key=value pair: '" + trimmedPair + "'. Expected format: key=value");
            }

            if (equalsIndex == 0) {
                throw new IllegalArgumentException("Empty key in key=value pair: '" + trimmedPair + "'");
            }

            String key = trimmedPair.substring(0, equalsIndex).trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Empty key in key=value pair: '" + trimmedPair + "'");
            }

            String rawValue = trimmedPair.substring(equalsIndex + 1);

            String value = unescapeString(rawValue).trim();

            result.put(key, value);
        }
        return result;
    }

    private int findFirstUnescapedChar(String input, char target) {
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == target) {
                return i;
            }
        }
        return -1;
    }

    private String unescapeString(String input) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) {
                result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }

        if (escaped) {
            result.append('\\');
        }

        return result.toString();
    }

    /**
     * Parses a string with escaped delimiters using backslash escape character. Supports escaping any
     * delimiter string with backslash.
     *
     * @param input The input string to parse
     * @param delimiter The delimiter to split on (can be multi-character)
     * @return List of parsed parts with escape sequences resolved
     */
    public List<String> parseEscapedString(String input, String delimiter) {
        List<String> result = new ArrayList<>();
        if (input == null) {
            return result;
        }

        if (input.trim().isEmpty()) {
            if (input.isEmpty()) {
                return result;
            }
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (input.substring(i).startsWith(delimiter)) {
                result.add(current.toString());
                current = new StringBuilder();
                i += delimiter.length() - 1;
            } else {
                current.append(c);
            }
        }

        // Handle trailing backslash - if escaped is still true, add the backslash literally
        if (escaped) {
            current.append('\\');
        }

        result.add(current.toString());

        return result;
    }
}
