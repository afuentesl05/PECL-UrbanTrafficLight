package com.example.apppecl3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        StrangerFx.attachIfEnabled(this);


        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnEnter       = findViewById(R.id.btnEnter);
        Button btnMqttConsole = findViewById(R.id.btnMqttConsole);

        UiUtils.setAnimatedClick(btnEnter, () -> {
            Intent intent = new Intent(MainActivity.this, StreetSelection.class);
            startActivity(intent);
        });

        UiUtils.setAnimatedClick(btnMqttConsole, () -> {
            Intent intent = new Intent(MainActivity.this, MqttConsoleActivity.class);
            startActivity(intent);
        });
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
