package ro.stancalau.test.framework.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class StringParsingUtils {
    
    /**
     * Parses a comma-separated string with support for escaped commas using backslash.
     * Example: "value1,value2,value with\, comma" -> ["value1", "value2", "value with, comma"]
     */
    public List<String> parseCommaSeparatedList(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return parseEscapedString(input, ",");
    }
    
    /**
     * Parses key=value pairs separated by commas with support for escaped characters.
     * Example: "key1=value1,key2=value with\, comma" -> {key1: "value1", key2: "value with, comma"}
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
                throw new IllegalArgumentException("Malformed key=value pair: '" + trimmedPair + "'. Expected format: key=value");
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
     * Parses a string with escaped delimiters using backslash escape character.
     * Supports escaping any delimiter string with backslash.
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