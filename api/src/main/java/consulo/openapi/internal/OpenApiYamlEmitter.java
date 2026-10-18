package consulo.openapi.internal;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class OpenApiYamlEmitter {
    private static final int INDENT = 2;
    private static final String INDICATORS = "-?:,[]{}#&*!|>'\"%@`";
    private static final Set<String> RESERVED = Set.of("true", "false", "null", "yes", "no", "on", "off", "~", "y", "n");
    private static final Pattern NUMBER = Pattern.compile("[-+]?(\\.?\\d[\\d_]*)(\\.\\d*)?([eE][-+]?\\d+)?|0x[0-9a-fA-F]+|0o[0-7]+|[-+]?\\.(inf|Inf|INF)|\\.(nan|NaN|NAN)");

    private OpenApiYamlEmitter() {
    }

    public static String emit(Map<String, ?> document) {
        StringBuilder builder = new StringBuilder();
        writeMap(builder, document, 0);
        return builder.toString();
    }

    private static void writeMap(StringBuilder builder, Map<?, ?> map, int indent) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            builder.append(" ".repeat(indent)).append(scalar(String.valueOf(entry.getKey()))).append(':');
            writeValue(builder, entry.getValue(), indent);
        }
    }

    private static void writeValue(StringBuilder builder, @Nullable Object value, int indent) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                builder.append(" {}\n");
            }
            else {
                builder.append('\n');
                writeMap(builder, map, indent + INDENT);
            }
        }
        else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                builder.append(" []\n");
            }
            else {
                builder.append('\n');
                writeList(builder, list, indent + INDENT);
            }
        }
        else {
            builder.append(' ').append(scalar(value)).append('\n');
        }
    }

    private static void writeList(StringBuilder builder, List<?> list, int indent) {
        String prefix = " ".repeat(indent);
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && !map.isEmpty()) {
                StringBuilder nested = new StringBuilder();
                writeMap(nested, map, indent + INDENT);
                builder.append(prefix).append("- ").append(nested, indent + INDENT, nested.length());
            }
            else if (item instanceof List<?> nestedList && !nestedList.isEmpty()) {
                builder.append(prefix).append("-\n");
                writeList(builder, nestedList, indent + INDENT);
            }
            else if (item instanceof Map<?, ?>) {
                builder.append(prefix).append("- {}\n");
            }
            else if (item instanceof List<?>) {
                builder.append(prefix).append("- []\n");
            }
            else {
                builder.append(prefix).append("- ").append(scalar(item)).append('\n');
            }
        }
    }

    private static String scalar(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        String text = value.toString();
        if (!needsQuotes(text)) {
            return text;
        }
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0 || text.indexOf('\t') >= 0) {
            return doubleQuoted(text);
        }
        return "'" + text.replace("'", "''") + "'";
    }

    private static boolean needsQuotes(String text) {
        if (text.isEmpty() || Character.isWhitespace(text.charAt(0)) || Character.isWhitespace(text.charAt(text.length() - 1))) {
            return true;
        }
        if (INDICATORS.indexOf(text.charAt(0)) >= 0) {
            return true;
        }
        if (text.contains(": ") || text.contains(" #") || text.endsWith(":")) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.isISOControl(text.charAt(i))) {
                return true;
            }
        }
        return RESERVED.contains(text.toLowerCase(Locale.ROOT)) || NUMBER.matcher(text).matches();
    }

    private static String doubleQuoted(String text) {
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (Character.isISOControl(c)) {
                        builder.append(String.format("\\x%02x", (int) c));
                    }
                    else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.append('"').toString();
    }
}
