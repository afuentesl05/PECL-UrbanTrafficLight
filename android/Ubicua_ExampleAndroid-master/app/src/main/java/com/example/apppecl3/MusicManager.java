package com.example.apppecl3;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class MusicManager {

    private static final String TAG = "MusicManager";

    private static int currentResId = R.raw.musica;

    private static MediaPlayer player;
    private static Context appContext;

    /**
     * Arranca (o reanuda) la música de fondo en bucle.
     */
    public static void start(Context context) {
        if (context == null) return;
        // Guardamos applicationContext
        appContext = context.getApplicationContext();
        int saved = ThemeManager.getSavedTrackResId(appContext, R.raw.musica);
        if (currentResId != saved) {
            currentResId = saved;
        }
        ThemeManager.setStrangerEnabled(appContext, isStrangerTrack(appContext, currentResId));
        Log.d(TAG, "start() player=" + player + " currentResId=" + currentResId);
        if (player == null) {
            createPlayer();
        }
        if (player != null && !player.isPlaying()) {
            try {
                player.start();
                Log.d(TAG, "Reproducción iniciada");
            } catch (Exception e) {
                Log.e(TAG, "Error al hacer start()", e);
            }
        }
    }

    /**
     * Cambia la pista de música y mantiene el mismo comportamiento de loop.
     */
    public static void setTrack(Context context, int resId) {
        if (context == null) return;

        appContext = context.getApplicationContext();

        ThemeManager.saveTrackResId(appContext, resId);

        boolean stranger = isStrangerTrack(appContext, resId);
        ThemeManager.setStrangerEnabled(appContext, stranger);

        if (player != null && currentResId == resId) {
            if (!player.isPlaying()) {
                try {
                    player.start();
                    Log.d(TAG, "Reanudando misma pista (resId=" + resId + ")");
                } catch (Exception e) {
                    Log.e(TAG, "Error al reanudar misma pista", e);
                }
            }
            return;
        }

        currentResId = resId;
        Log.d(TAG, "Cambiando pista. Nuevo resId=" + resId + " stranger=" + stranger);

        releaseInternal();
        createPlayer();

        if (player != null) {
            try {
                player.start();
                Log.d(TAG, "Reproducción iniciada tras cambiar de pista");
            } catch (Exception e) {
                Log.e(TAG, "Error al hacer start() tras cambiar pista", e);
            }
        }
    }

    private static boolean isStrangerTrack(Context context, int resId) {
        try {
            String key = context.getResources().getResourceEntryName(resId);
            return "st".equalsIgnoreCase(key) || "kids".equalsIgnoreCase(key);
        } catch (Exception e) {
            return false;
        }
    }

    private static void createPlayer() {
        if (appContext == null) {
            Log.e(TAG, "createPlayer() llamado sin appContext");
            return;
        }

        Log.d(TAG, "Creando MediaPlayer con resId=" + currentResId);
        player = MediaPlayer.create(appContext, currentResId);

        if (player == null) {
            Log.e(TAG, "No se ha podido crear el MediaPlayer (res/raw)");
            return;
        }

        // Loop manual recreando el player
        player.setOnCompletionListener(mp -> {
            Log.d(TAG, "onCompletion() -> loop manual");

            try { mp.release(); } catch (Exception ignored) {}
            player = null;

            createPlayer();
            if (player != null) {
                try { player.start(); } catch (Exception ignored) {}
            }
        });
    }

    public static void pause() {
        Log.d(TAG, "pause()");
        if (player != null && player.isPlaying()) {
            try {
                player.pause();
            } catch (Exception e) {
                Log.e(TAG, "Error al pausar", e);
            }
        }
    }

    public static void release() {
        Log.d(TAG, "release()");
        releaseInternal();
    }

    private static void releaseInternal() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.pause();
                player.release();
            } catch (Exception e) {
                Log.e(TAG, "Error al liberar", e);
            } finally {
                player = null;
            }
        }
    }

    public static int getCurrentResId() {
        return currentResId;
    }
}
