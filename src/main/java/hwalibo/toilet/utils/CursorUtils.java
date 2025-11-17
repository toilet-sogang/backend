package hwalibo.toilet.utils;


import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import java.time.OffsetDateTime;
import java.util.Base64;

public final class CursorUtils {

    private CursorUtils() {}

    public static String encode(LocalDateTime createdAt, Long id) {
        // createdAt|id  → URL-safe Base64
        String raw = createdAt.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static record Cursor(LocalDateTime createdAt, Long id) {}

    public static Cursor decode(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|");
            if (parts.length != 2) throw new IllegalArgumentException("invalid cursor format");
            return new Cursor( LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed nextCursor", e);
        }
    }
}
