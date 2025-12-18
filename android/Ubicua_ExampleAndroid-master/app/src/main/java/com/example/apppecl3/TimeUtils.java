package com.example.apppecl3;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeUtils {

    private TimeUtils() {}

    // Entrada del servidor: "Dec 13, 2025, 4:33:55 PM"
    private static final String IN_FMT = "MMM d, yyyy, h:mm:ss a";
    private static final String OUT_FMT = "dd/MM/yyyy HH:mm:ss";

    public static String toMadrid(String raw) {
        if (raw == null) return "-";
        raw = raw.trim();
        if (raw.isEmpty()) return "-";
        if (raw.contains("T") && (raw.contains("+") || raw.endsWith("Z"))) {
            return raw;
        }

        try {
            SimpleDateFormat in = new SimpleDateFormat(IN_FMT, Locale.US);
            in.setLenient(true);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date parsed = in.parse(raw);

            SimpleDateFormat out = new SimpleDateFormat(OUT_FMT, Locale.getDefault());
            out.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));

            return (parsed != null) ? out.format(parsed) : raw;

        } catch (ParseException e) {
            return raw;
        }
    }
}
