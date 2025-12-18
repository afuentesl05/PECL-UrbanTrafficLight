package com.example.apppecl3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private final List<String>  trackNames    = new ArrayList<>();
    private final List<String>  trackRawNames = new ArrayList<>();
    private final List<Integer> trackResIds   = new ArrayList<>();

    private TextView textCurrentMusic;
    private MaterialButton btnPickMusic;
    private MaterialButton btnGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ===== Toolbar =====
        MaterialToolbar top = findViewById(R.id.topAppBar);
        top.setNavigationOnClickListener(v -> finish());

        // ===== Música =====
        textCurrentMusic = findViewById(R.id.textCurrentMusic);
        btnPickMusic     = findViewById(R.id.btnPickMusic);

        // ===== Botón Juego =====
        btnGame = findViewById(R.id.btnGame);
        UiUtils.setAnimatedClick(btnGame, () ->
                startActivity(new Intent(SettingsActivity.this, GameActivity.class))
        );

        initMusicTracks();

        boolean changedBySync = syncStrangerWithCurrentTrack();
        if (changedBySync) {
            ThemeManager.apply(this);
            restartClean();
            return;
        }

        StrangerFx.attachIfEnabled(this);

        updateCurrentMusicLabel();
        UiUtils.setAnimatedClick(btnPickMusic, this::showMusicPicker);

        RadioGroup rg = findViewById(R.id.radioGroupTheme);

        String current = ThemeManager.getTheme(this);
        if ("light".equals(current)) rg.check(R.id.radioLight);
        else if ("dark".equals(current)) rg.check(R.id.radioDark);
        else rg.check(R.id.radioCyber);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            String selected;
            if (checkedId == R.id.radioLight) selected = "light";
            else if (checkedId == R.id.radioDark) selected = "dark";
            else selected = "cyber";

            if (selected.equals(ThemeManager.getTheme(this))) return;
            if (selected.equals(ThemeManager.getTheme(this))) return;

            ThemeManager.saveTheme(this, selected);

            ThemeManager.apply(this);
            restartClean();
        });


    }

    private void initMusicTracks() {
        trackNames.clear();
        trackRawNames.clear();
        trackResIds.clear();

        try {
            Field[] fields = R.raw.class.getDeclaredFields();
            for (Field f : fields) {
                try {
                    int resId = f.getInt(null);
                    String rawName = f.getName();

                    String prettyName = rawName.replace('_', ' ');
                    if (!prettyName.isEmpty()) {
                        prettyName = prettyName.substring(0, 1).toUpperCase() + prettyName.substring(1);
                    }

                    trackNames.add(prettyName);
                    trackRawNames.add(rawName);
                    trackResIds.add(resId);

                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Error accediendo a R.raw", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reflexionando sobre R.raw", e);
        }

        if (trackResIds.isEmpty()) {
            Log.w(TAG, "No se han encontrado recursos en R.raw.");
        }
    }

    private void updateCurrentMusicLabel() {
        int currentRes = MusicManager.getCurrentResId();
        String label = "-";

        for (int i = 0; i < trackResIds.size(); i++) {
            if (trackResIds.get(i) == currentRes) {
                label = trackNames.get(i);
                break;
            }
        }
        textCurrentMusic.setText("Actual: " + label);
    }

    private void showMusicPicker() {
        if (trackNames.isEmpty() || trackResIds.isEmpty()) {
            Toast.makeText(this, "No se han encontrado pistas en res/raw", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] namesArray = trackNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Selecciona la música de fondo")
                .setItems(namesArray, (dialog, which) -> {
                    if (which < 0 || which >= trackResIds.size()) return;

                    int resId = trackResIds.get(which);

                    boolean strangerBefore = ThemeManager.isStrangerEnabled(getApplicationContext());

                    // Cambia pista (MusicManager ya actualiza ThemeManager.setStrangerEnabled si es st/kids)
                    MusicManager.setTrack(this, resId);

                    boolean strangerAfter = ThemeManager.isStrangerEnabled(getApplicationContext());

                    updateCurrentMusicLabel();

                    Toast.makeText(this, "Pista seleccionada: " + namesArray[which], Toast.LENGTH_SHORT).show();

                    if (strangerBefore != strangerAfter) {
                        ThemeManager.apply(this);
                        restartClean();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean isStrangerTrack(String rawName) {
        if (rawName == null) return false;
        String n = rawName.trim().toLowerCase();
        return n.equals("st") || n.equals("kids");
    }

    private boolean syncStrangerWithCurrentTrack() {
        int currentRes = MusicManager.getCurrentResId();

        String rawName = null;
        for (int i = 0; i < trackResIds.size(); i++) {
            if (trackResIds.get(i) == currentRes) {
                rawName = trackRawNames.get(i);
                break;
            }
        }

        boolean shouldBe = isStrangerTrack(rawName);
        boolean before = ThemeManager.isStrangerEnabled(getApplicationContext());

        if (before != shouldBe) {
            ThemeManager.setStrangerEnabled(getApplicationContext(), shouldBe);
            Log.d(TAG, "syncStrangerWithCurrentTrack(): " + before + " -> " + shouldBe);
            return true;
        }
        return false;
    }

    private void restartClean() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.start(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pause();
    }
}
