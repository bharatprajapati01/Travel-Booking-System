package travel.core;

import java.util.*;

/** Minimal JSON writer + flat-object parser (no external dependencies). */
public final class Json {
    private Json() {}

    public static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        write(sb, o);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object o) {
        if (o == null) sb.append("null");
        else if (o instanceof String) quote(sb, (String) o);
        else if (o instanceof Number || o instanceof Boolean) sb.append(o);
        else if (o instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof Iterable) {
            sb.append('[');
            boolean first = true;
            for (Object x : (Iterable<?>) o) {
                if (!first) sb.append(',');
                first = false;
                write(sb, x);
            }
            sb.append(']');
        } else quote(sb, o.toString());
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    /** Parses a flat JSON object into a map of String -> String (numbers/booleans kept as text, null as null). */
    public static Map<String, String> parseObject(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        int[] i = {0};
        skipWs(s, i);
        expect(s, i, '{');
        skipWs(s, i);
        if (peek(s, i) == '}') { i[0]++; return m; }
        while (true) {
            skipWs(s, i);
            String key = readString(s, i);
            skipWs(s, i);
            expect(s, i, ':');
            skipWs(s, i);
            m.put(key, readValue(s, i));
            skipWs(s, i);
            char c = peek(s, i);
            i[0]++;
            if (c == '}') break;
            if (c != ',') throw new IllegalArgumentException("Invalid JSON");
        }
        return m;
    }

    private static char peek(String s, int[] i) {
        if (i[0] >= s.length()) throw new IllegalArgumentException("Invalid JSON");
        return s.charAt(i[0]);
    }

    private static void skipWs(String s, int[] i) {
        while (i[0] < s.length() && Character.isWhitespace(s.charAt(i[0]))) i[0]++;
    }

    private static void expect(String s, int[] i, char c) {
        if (peek(s, i) != c) throw new IllegalArgumentException("Invalid JSON");
        i[0]++;
    }

    private static String readValue(String s, int[] i) {
        char c = peek(s, i);
        if (c == '"') return readString(s, i);
        int start = i[0];
        while (i[0] < s.length() && ",}".indexOf(s.charAt(i[0])) < 0) i[0]++;
        String raw = s.substring(start, i[0]).trim();
        if (raw.isEmpty() || raw.equals("null")) return null;
        if (raw.startsWith("{") || raw.startsWith("[")) throw new IllegalArgumentException("Nested JSON not supported");
        return raw;
    }

    private static String readString(String s, int[] i) {
        expect(s, i, '"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = peek(s, i);
            i[0]++;
            if (c == '"') break;
            if (c == '\\') {
                char n = peek(s, i);
                i[0]++;
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (i[0] + 4 > s.length()) throw new IllegalArgumentException("Invalid JSON");
                        sb.append((char) Integer.parseInt(s.substring(i[0], i[0] + 4), 16));
                        i[0] += 4;
                        break;
                    default: sb.append(n);
                }
            } else sb.append(c);
        }
        return sb.toString();
    }
}
