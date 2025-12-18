package com.example.apppecl3;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeManager {

    public static final String PREFS = "ui_prefs";
    public static final String KEY_THEME = "theme"; // "light" | "dark" | "cyber"

    public static final String KEY_STRANGER = "stranger_mode";

    public static final String KEY_MUSIC_RES_ID = "music_res_id";

    private ThemeManager(){}

    public static void apply(Activity activity) {
        syncStrangerWithSavedTrack(activity);

        String theme = getTheme(activity);
        boolean stranger = isStrangerEnabled(activity);

        // Night mode base
        if (stranger) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        // Cyber overlay
        if ("cyber".equals(theme)) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_AppPECL3_Cyber, true);
        }

        // Stranger overlay (prioridad sobre Cyber)
        if (stranger) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_AppPECL3_Stranger, true);
        }
    }

    public static void saveTheme(Context context, String theme) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_THEME, theme).apply();
    }

    public static String getTheme(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_THEME, "dark");
    }

    // ===== Stranger =====

    public static void setStrangerEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_STRANGER, enabled).apply();
    }

    public static boolean isStrangerEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_STRANGER, false);
    }

    // ===== Música guardada =====

    public static void saveTrackResId(Context context, int resId) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_MUSIC_RES_ID, resId).apply();
    }

    public static int getSavedTrackResId(Context context, int defaultResId) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getInt(KEY_MUSIC_RES_ID, defaultResId);
    }


    private static void syncStrangerWithSavedTrack(Context context) {
        int savedResId = getSavedTrackResId(context, R.raw.musica);

        boolean shouldBeStranger = false;
        try {
            String rawName = context.getResources().getResourceEntryName(savedResId);
            shouldBeStranger = "st".equalsIgnoreCase(rawName) || "kids".equalsIgnoreCase(rawName);
        } catch (Exception ignored) {
            // Si algo falla, no activamos Stranger
            shouldBeStranger = false;
        }

        boolean current = isStrangerEnabled(context);
        if (current != shouldBeStranger) {
            setStrangerEnabled(context, shouldBeStranger);
        }
    }
}
