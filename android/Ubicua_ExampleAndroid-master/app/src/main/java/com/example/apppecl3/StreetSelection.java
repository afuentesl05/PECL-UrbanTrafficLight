package com.example.apppecl3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;
import android.widget.AdapterView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreetSelection extends AppCompatActivity {

    private Spinner spinnerStreet;
    private Spinner spinnerDevice;

    private Button botonContinuar;
    private Button btnForcePed;
    private Button btnBuzzerOn;
    private Button btnBuzzerOff;

    private final List<String>  streetIds   = new ArrayList<>();
    private final List<Integer> deviceIds   = new ArrayList<>();

    private ArrayAdapter<String> streetsAdapter;
    private ArrayAdapter<String> devicesAdapter;

    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_street_selection);
        StrangerFx.attachIfEnabled(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerStreet = findViewById(R.id.spinnerStreet);
        spinnerDevice = findViewById(R.id.spinner);

        botonContinuar = findViewById(R.id.button);
        btnForcePed    = findViewById(R.id.btnForcePed);
        btnBuzzerOn    = findViewById(R.id.btnBuzzerOn);
        btnBuzzerOff   = findViewById(R.id.btnBuzzerOff);

        api = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        setupAdapters();
        setupListeners();

        loadStreets();
    }

    private void setupAdapters() {
        streetsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        streetsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStreet.setAdapter(streetsAdapter);

        devicesAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        devicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDevice.setAdapter(devicesAdapter);
    }

    private void setupListeners() {
        spinnerStreet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String streetId = getSelectedStreetId();
                if (streetId == null) return;
                loadDevicesForStreet(streetId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        UiUtils.setAnimatedClick(botonContinuar, () -> {
            String streetId = getSelectedStreetId();
            Integer deviceId = getSelectedDeviceId();
            if (streetId == null || deviceId == null) return;

            Intent intent = new Intent(StreetSelection.this, StreetMonitoring.class);
            intent.putExtra("street_id", streetId);
            intent.putExtra("device_id", deviceId);
            startActivity(intent);
        });

        UiUtils.setAnimatedClick(btnForcePed, () -> {
            String streetId = getSelectedStreetId();
            Integer deviceId = getSelectedDeviceId();
            if (streetId == null || deviceId == null) return;
            sendForcePedestrian(streetId, deviceId);
        });

        UiUtils.setAnimatedClick(btnBuzzerOn, () -> {
            String streetId = getSelectedStreetId();
            Integer deviceId = getSelectedDeviceId();
            if (streetId == null || deviceId == null) return;
            sendBuzzerCommand(streetId, deviceId, true);
        });

        UiUtils.setAnimatedClick(btnBuzzerOff, () -> {
            String streetId = getSelectedStreetId();
            Integer deviceId = getSelectedDeviceId();
            if (streetId == null || deviceId == null) return;
            sendBuzzerCommand(streetId, deviceId, false);
        });
    }

    private void loadStreets() {
        api.getStreets().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StreetSelection.this,
                            "Error GetStreets: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                streetIds.clear();
                streetIds.addAll(response.body());

                List<String> pretty = new ArrayList<>();
                for (String s : streetIds) pretty.add(s);

                streetsAdapter.clear();
                streetsAdapter.addAll(pretty);
                streetsAdapter.notifyDataSetChanged();

                if (streetIds.isEmpty()) {
                    Toast.makeText(StreetSelection.this,
                            "No hay calles en BD",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Log.e("ubicua", "GetStreets fail", t);
                Toast.makeText(StreetSelection.this,
                        "Error de red GetStreets: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDevicesForStreet(String streetId) {
        api.getDevicesByStreet(streetId).enqueue(new Callback<List<Integer>>() {
            @Override
            public void onResponse(Call<List<Integer>> call, Response<List<Integer>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(StreetSelection.this,
                            "Error GetDevicesByStreet: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                deviceIds.clear();
                deviceIds.addAll(response.body());

                List<String> pretty = new ArrayList<>();
                for (Integer id : deviceIds) pretty.add("Dispositivo " + id);

                devicesAdapter.clear();
                devicesAdapter.addAll(pretty);
                devicesAdapter.notifyDataSetChanged();

                if (deviceIds.isEmpty()) {
                    Toast.makeText(StreetSelection.this,
                            "No hay dispositivos para " + streetId,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Integer>> call, Throwable t) {
                Log.e("ubicua", "GetDevicesByStreet fail", t);
                Toast.makeText(StreetSelection.this,
                        "Error de red GetDevicesByStreet: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSelectedStreetId() {
        int pos = spinnerStreet.getSelectedItemPosition();
        if (pos < 0 || pos >= streetIds.size()) {
            Toast.makeText(this, "Selecciona una calle", Toast.LENGTH_SHORT).show();
            return null;
        }
        return streetIds.get(pos);
    }

    private Integer getSelectedDeviceId() {
        int pos = spinnerDevice.getSelectedItemPosition();
        if (pos < 0 || pos >= deviceIds.size()) {
            Toast.makeText(this, "Selecciona un dispositivo", Toast.LENGTH_SHORT).show();
            return null;
        }
        return deviceIds.get(pos);
    }

    private void sendForcePedestrian(String streetId, int deviceId) {
        api.forcePedestrian("force", streetId, deviceId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(StreetSelection.this,
                                    "Forzado peatones (" + streetId + ", TL_" + deviceId + ")",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(StreetSelection.this,
                                    "Error force: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(StreetSelection.this,
                                "Error de red force: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendBuzzerCommand(String streetId, int deviceId, boolean enabled) {
        api.setBuzzer("buzzer", enabled, streetId, deviceId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(StreetSelection.this,
                                    "Buzzer " + (enabled ? "ON" : "OFF")
                                            + " (" + streetId + ", TL_" + deviceId + ")",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(StreetSelection.this,
                                    "Error buzzer: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(StreetSelection.this,
                                "Error de red buzzer: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
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
