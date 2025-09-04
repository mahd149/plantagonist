// src/main/java/org/plantagonist/core/json/DateAdapters.java
package org.plantagonist.core.json;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public final class DateAdapters {

    // Canonical write format (ISO-8601 with millis & zone)
    public static final String ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    // Accept these legacy/read formats
    private static final List<String> LEGACY_PATTERNS = List.of(
            ISO,                                // ISO w/ millis
            "yyyy-MM-dd'T'HH:mm:ssXXX",         // ISO no millis
            "MMM d, yyyy, h:mm:ss a"            // Gson default-ish
    );

    private static final ThreadLocal<DateFormat> ISO_WRITER =
            ThreadLocal.withInitial(() -> new SimpleDateFormat(ISO, Locale.US));

    private static String normalizeSpaces(String s) {
        if (s == null) return null;
        // Replace narrow NBSP (U+202F), NBSP (U+00A0), any Unicode space with normal space
        s = s.replace('\u202F',' ').replace('\u00A0',' ');
        s = s.replaceAll("\\p{Z}+", " ");    // all separator spaces -> ' '
        s = s.replaceAll("\\s+", " ");       // collapse
        return s.trim();
    }

    public static final JsonDeserializer<Date> DESERIALIZER = new JsonDeserializer<>() {
        @Override public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            String raw = json.getAsString();
            String s = normalizeSpaces(raw);

            // Try java.time first (best tolerance with optional millis/zone)
            try {
                DateTimeFormatter f = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                        .optionalStart().appendPattern(".SSS").optionalEnd()
                        .appendOffset("+HH:MM", "Z")
                        .toFormatter(Locale.US);
                return Date.from(OffsetDateTime.parse(s, f).toInstant());
            } catch (Exception ignored) {}

            // Try legacy SimpleDateFormat patterns (including "MMM d, yyyy, h:mm:ss a")
            for (String p : LEGACY_PATTERNS) {
                try {
                    SimpleDateFormat df = new SimpleDateFormat(p, Locale.US);
                    df.setLenient(true);
                    return df.parse(s);
                } catch (ParseException ignored) {}
            }

            throw new JsonParseException("Unrecognized date: " + raw);
        }
    };

    public static final JsonSerializer<Date> SERIALIZER = new JsonSerializer<>() {
        @Override public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext ctx) {
            return new JsonPrimitive(ISO_WRITER.get().format(src));
        }
    };

    private DateAdapters() {}
}
