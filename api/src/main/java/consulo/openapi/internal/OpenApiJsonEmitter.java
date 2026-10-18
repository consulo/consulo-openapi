package consulo.openapi.internal;

import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class OpenApiJsonEmitter {
    private static final String INDENT = "  ";

    private OpenApiJsonEmitter() {
    }

    public static String emit(Map<String, ?> document) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, document, 0);
        return builder.append('\n').toString();
    }

    private static void writeValue(StringBuilder builder, @Nullable Object value, int depth) {
        if (value == null) {
            builder.append("null");
        }
        else if (value instanceof Map<?, ?> map) {
            writeMap(builder, map, depth);
        }
        else if (value instanceof List<?> list) {
            writeList(builder, list, depth);
        }
        else if (value instanceof Boolean || value instanceof Number) {
            builder.append(value);
        }
        else {
            writeString(builder, value.toString());
        }
    }

    private static void writeMap(StringBuilder builder, Map<?, ?> map, int depth) {
        if (map.isEmpty()) {
            builder.append("{}");
            return;
        }
        builder.append("{\n");
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            builder.append(INDENT.repeat(depth + 1));
            writeString(builder, String.valueOf(entry.getKey()));
            builder.append(": ");
            writeValue(builder, entry.getValue(), depth + 1);
            if (iterator.hasNext()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append(INDENT.repeat(depth)).append('}');
    }

    private static void writeList(StringBuilder builder, List<?> list, int depth) {
        if (list.isEmpty()) {
            builder.append("[]");
            return;
        }
        builder.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            builder.append(INDENT.repeat(depth + 1));
            writeValue(builder, list.get(i), depth + 1);
            if (i < list.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append(INDENT.repeat(depth)).append(']');
    }

    private static void writeString(StringBuilder builder, String text) {
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    }
                    else {
                        builder.append(c);
                    }
                }
            }
        }
        builder.append('"');
    }
}
