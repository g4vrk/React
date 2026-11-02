package com.g4vrk.react.inference.json;

import com.g4vrk.react.player.model.rotation.Rotation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InferenceJsonCodec {

    private static final int MAX_NESTING_DEPTH = 64;

    private InferenceJsonCodec() {
    }

    public static @NotNull String writeObject(final @NotNull Map<String, ?> values) {
        final StringBuilder output = new StringBuilder(256);
        appendObject(output, values, 0);
        return output.toString();
    }

    public static @NotNull Map<String, Object> readObject(final @NotNull String json) {
        final Parser parser = new Parser(json);
        final Object value = parser.readValue(0);
        parser.skipWhitespace();

        if (parser.hasRemaining()) {
            throw parser.error("Unexpected trailing content");
        }

        if (!(value instanceof Map<?, ?> map)) {
            throw parser.error("Expected a JSON object");
        }

        final Map<String, Object> result = new LinkedHashMap<>(map.size());

        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw parser.error("JSON object contains a non-string key");
            }

            result.put(key, entry.getValue());
        }

        return result;
    }

    private static void appendObject(
            final @NotNull StringBuilder output,
            final @NotNull Map<String, ?> values,
            final int depth
    ) {
        checkDepth(depth);

        output.append('{');

        boolean first = true;

        for (final Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                output.append(',');
            }

            first = false;
            appendString(output, entry.getKey());
            output.append(':');
            appendValue(output, entry.getValue(), depth + 1);
        }

        output.append('}');
    }

    private static void appendValue(
            final @NotNull StringBuilder output,
            final Object value,
            final int depth
    ) {
        checkDepth(depth);

        if (value == null) {
            output.append("null");
            return;
        }

        if (value instanceof String string) {
            appendString(output, string);
            return;
        }

        if (value instanceof Boolean) {
            output.append(value);
            return;
        }

        if (value instanceof Number number) {
            appendNumber(output, number);
            return;
        }

        if (value instanceof Rotation rotation) {
            appendRotation(output, rotation, depth + 1);
            return;
        }

        if (value instanceof Map<?, ?> map) {
            appendMap(output, map, depth + 1);
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            appendIterable(output, iterable, depth + 1);
            return;
        }

        if (value.getClass().isArray()) {
            appendArray(output, value, depth + 1);
            return;
        }

        throw new IllegalArgumentException(
                "Unsupported inference JSON value type: " + value.getClass().getName()
        );
    }

    private static void appendRotation(
            final @NotNull StringBuilder output,
            final @NotNull Rotation rotation,
            final int depth
    ) {
        checkDepth(depth);

        output.append('{');
        appendProperty(output, "y", rotation.getY());
        appendProperty(output, "x", rotation.getX());
        appendProperty(output, "deltaY", rotation.getDeltaY());
        appendProperty(output, "deltaX", rotation.getDeltaX());
        appendProperty(output, "jerkY", rotation.getJerkY());
        appendProperty(output, "jerkX", rotation.getJerkX());
        appendProperty(output, "gcdErrorY", rotation.getGcdErrorY());
        appendProperty(output, "gcdErrorX", rotation.getGcdErrorX());
        output.setLength(output.length() - 1);
        output.append('}');
    }

    private static void appendProperty(
            final @NotNull StringBuilder output,
            final @NotNull String name,
            final float value
    ) {
        appendString(output, name);
        output.append(':');
        appendNumber(output, value);
        output.append(',');
    }

    private static void appendMap(
            final @NotNull StringBuilder output,
            final @NotNull Map<?, ?> values,
            final int depth
    ) {
        checkDepth(depth);

        output.append('{');
        boolean first = true;

        for (final Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("JSON object keys must be strings");
            }

            if (!first) {
                output.append(',');
            }

            first = false;
            appendString(output, key);
            output.append(':');
            appendValue(output, entry.getValue(), depth + 1);
        }

        output.append('}');
    }

    private static void appendIterable(
            final @NotNull StringBuilder output,
            final @NotNull Iterable<?> values,
            final int depth
    ) {
        checkDepth(depth);

        output.append('[');
        boolean first = true;

        for (final Object value : values) {
            if (!first) {
                output.append(',');
            }

            first = false;
            appendValue(output, value, depth + 1);
        }

        output.append(']');
    }

    private static void appendArray(
            final @NotNull StringBuilder output,
            final @NotNull Object values,
            final int depth
    ) {
        checkDepth(depth);

        output.append('[');

        for (int i = 0, length = Array.getLength(values); i < length; i++) {
            if (i > 0) {
                output.append(',');
            }

            appendValue(output, Array.get(values, i), depth + 1);
        }

        output.append(']');
    }

    private static void appendNumber(
            final @NotNull StringBuilder output,
            final @NotNull Number number
    ) {
        if (number instanceof Double value && !Double.isFinite(value)
                || number instanceof Float value && !Float.isFinite(value)) {
            throw new IllegalArgumentException("JSON numbers must be finite");
        }

        output.append(number);
    }

    private static void appendString(
            final @NotNull StringBuilder output,
            final @NotNull String value
    ) {
        output.append('"');

        for (int i = 0, length = value.length(); i < length; i++) {
            final char character = value.charAt(i);

            switch (character) {
                case '"' -> output.append("\\\"");

                case '\\' -> output.append("\\\\");

                case '\b' -> output.append("\\b");

                case '\f' -> output.append("\\f");

                case '\n' -> output.append("\\n");

                case '\r' -> output.append("\\r");

                case '\t' -> output.append("\\t");

                default -> {
                    if (character < 0x20) {
                        output.append("\\u");
                        output.append(HEX[(character >>> 12) & 0xF]);
                        output.append(HEX[(character >>> 8) & 0xF]);
                        output.append(HEX[(character >>> 4) & 0xF]);
                        output.append(HEX[character & 0xF]);
                    } else {
                        output.append(character);
                    }
                }
            }
        }

        output.append('"');
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static void checkDepth(final int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new IllegalArgumentException("JSON nesting depth exceeds " + MAX_NESTING_DEPTH);
        }
    }

    private static final class Parser {

        private final String input;
        private int index;

        private Parser(final @NotNull String input) {
            this.input = input;
        }

        private Object readValue(final int depth) {
            checkDepth(depth);
            skipWhitespace();

            if (!hasRemaining()) {
                throw error("Expected a JSON value");
            }

            return switch (input.charAt(index)) {
                case '{' -> readObject(depth + 1);
                case '[' -> readArray(depth + 1);
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject(final int depth) {
            checkDepth(depth);
            expect('{');
            skipWhitespace();

            final Map<String, Object> values = new LinkedHashMap<>();

            if (consume('}')) {
                return values;
            }

            while (true) {
                skipWhitespace();

                if (!hasRemaining() || input.charAt(index) != '"') {
                    throw error("Expected a string key");
                }

                final String key = readString();
                skipWhitespace();
                expect(':');

                final Object value = readValue(depth + 1);
                values.put(key, value);

                skipWhitespace();

                if (consume('}')) {
                    return values;
                }

                expect(',');
            }
        }

        private List<Object> readArray(final int depth) {
            checkDepth(depth);
            expect('[');
            skipWhitespace();

            final List<Object> values = new ArrayList<>();

            if (consume(']')) {
                return values;
            }

            while (true) {
                values.add(readValue(depth + 1));
                skipWhitespace();

                if (consume(']')) {
                    return values;
                }

                expect(',');
            }
        }

        private String readString() {
            expect('"');

            final StringBuilder value = new StringBuilder();

            while (hasRemaining()) {
                final char character = input.charAt(index++);

                if (character == '"') {
                    return value.toString();
                }

                if (character < 0x20) {
                    throw error("Unescaped control character in string");
                }

                if (character != '\\') {
                    value.append(character);
                    continue;
                }

                if (!hasRemaining()) {
                    throw error("Incomplete escape sequence");
                }

                final char escaped = input.charAt(index++);

                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(readUnicodeCharacter());
                    default -> throw error("Invalid escape sequence");
                }
            }

            throw error("Unterminated string");
        }

        private char readUnicodeCharacter() {
            if (index + 4 > input.length()) {
                throw error("Incomplete Unicode escape");
            }

            int value = 0;

            for (int i = 0; i < 4; i++) {
                final int digit = Character.digit(input.charAt(index++), 16);

                if (digit == -1) {
                    throw error("Invalid Unicode escape");
                }

                value = (value << 4) | digit;
            }

            return (char) value;
        }

        private Object readLiteral(
                final @NotNull String literal,
                final Object value
        ) {
            if (!input.startsWith(literal, index)) {
                throw error("Invalid JSON literal");
            }

            index += literal.length();
            return value;
        }

        private BigDecimal readNumber() {
            final int start = index;

            consume('-');

            if (consume('0')) {
                if (hasDigit()) {
                    throw error("Leading zero in number");
                }
            } else {
                readDigits(true);
            }

            if (consume('.')) {
                readDigits(true);
            }

            if (consume('e') || consume('E')) {
                if (!consume('+')) {
                    consume('-');
                }

                readDigits(true);
            }

            try {
                return new BigDecimal(input.substring(start, index));
            } catch (final NumberFormatException ex) {
                throw error("Invalid JSON number");
            }
        }

        private void readDigits(final boolean required) {
            final int start = index;

            while (hasDigit()) {
                index++;
            }

            if (required && start == index) {
                throw error("Expected a digit");
            }
        }

        private boolean hasDigit() {
            if (!hasRemaining()) {
                return false;
            }

            final char character = input.charAt(index);
            return character >= '0' && character <= '9';
        }

        private void skipWhitespace() {
            while (hasRemaining()) {
                final char character = input.charAt(index);

                if (character != ' ' && character != '\n'
                        && character != '\r' && character != '\t') {
                    return;
                }

                index++;
            }
        }

        private void expect(final char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private boolean consume(final char expected) {
            if (!hasRemaining() || input.charAt(index) != expected) {
                return false;
            }

            index++;
            return true;
        }

        private boolean hasRemaining() {
            return index < input.length();
        }

        private @NotNull IllegalArgumentException error(final @NotNull String message) {
            return new IllegalArgumentException(message + " at index " + index);
        }
    }
}
