package com.example.apppecl3;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreetMonitoring extends AppCompatActivity {

    // NUEVO: calle + dispositivo
    private String streetId;     // "ST_2245"
    private int deviceId;        // 1,2,3...
    private String streetName;

    private MqttClient client;

    // Cabecera
    private TextView textDevice;
    private TextView textLastState;
    private TextView textLastTimestamp;

    // Histórico
    private RecyclerView recyclerHistory;
    private MeasurementAdapter adapter;
    private final List<MeasurementDto> history = new ArrayList<>();

    // Filtros por fecha
    private TextView textStartDate;
    private TextView textEndDate;
    private Button btnApplyFilter;
    private Button btnClearFilter;

    // Valores filtro (null = sin filtro)
    private String startDateFilter = null; // "yyyy-MM-ddTHH:mm"
    private String endDateFilter   = null;

    // Broker MQTT
    private static final String MQTT_BROKER_URI = "tcp://192.168.0.73:1883"; //IP DEL BROKER


    // Topic de estado: sensors/<streetId>/traffic_light/TL_00X/state
    private static final String TOPIC_TEMPLATE =
            "sensors/%s/traffic_light/TL_%03d/state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_street_monitoring);
        StrangerFx.attachIfEnabled(this);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ===== Views =====
        textDevice        = findViewById(R.id.textDevice);
        textLastState     = findViewById(R.id.textLastState);
        textLastTimestamp = findViewById(R.id.textLastTimestamp);

        textStartDate  = findViewById(R.id.textStartDate);
        textEndDate    = findViewById(R.id.textEndDate);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnClearFilter = findViewById(R.id.btnClearFilter);

        recyclerHistory = findViewById(R.id.recyclerHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MeasurementAdapter(history);
        recyclerHistory.setAdapter(adapter);

        // ===== Extras =====
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            textLastState.setText("Error: no se han recibido datos en el Intent.");
            return;
        }

        streetId   = extras.getString("street_id", null);
        deviceId   = extras.getInt("device_id", -1);
        streetName = extras.getString("street_name", "");

        if (streetId == null || streetId.isBlank() || deviceId == -1) {
            Log.w("ubicua", "street_id o device_id no recibido correctamente.");
            textLastState.setText("Error: faltan street_id o device_id.");
            return;
        }

        String header = "Calle " + streetId + " • Dispositivo " + deviceId;
        if (streetName != null && !streetName.isBlank()) header += " - " + streetName;
        textDevice.setText(header);

        final String topic = String.format(Locale.US, TOPIC_TEMPLATE, streetId, deviceId);
        Log.i("ubicua", "Monitorizando: streetId=" + streetId + " deviceId=" + deviceId + " topic=" + topic);

        // ===== Clicks =====
        UiUtils.setAnimatedClick(textStartDate, () -> mostrarDatePicker(true));
        UiUtils.setAnimatedClick(textEndDate,   () -> mostrarDatePicker(false));

        UiUtils.setAnimatedClick(btnApplyFilter, () ->
                cargarHistoricoDesdeServidor(deviceId, startDateFilter, endDateFilter)
        );

        UiUtils.setAnimatedClick(btnClearFilter, () -> {
            startDateFilter = null;
            endDateFilter   = null;
            textStartDate.setText("Desde: (sin filtro)");
            textEndDate.setText("Hasta: (sin filtro)");
            cargarHistoricoDesdeServidor(deviceId, null, null);
        });

        // ===== Carga inicial =====
        cargarHistoricoDesdeServidor(deviceId, null, null);
        new Thread(() -> conectarMqtt(topic)).start();
    }

    private void mostrarDatePicker(boolean esFechaInicio) {
        final Calendar c = Calendar.getInstance();
        int year  = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day   = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    String fechaBonita = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d", d, m + 1, y);

                    if (esFechaInicio) {
                        startDateFilter = String.format(Locale.US,
                                "%04d-%02d-%02dT00:00", y, m + 1, d);
                        textStartDate.setText("Desde: " + fechaBonita);
                    } else {
                        endDateFilter = String.format(Locale.US,
                                "%04d-%02d-%02dT23:59", y, m + 1, d);
                        textEndDate.setText("Hasta: " + fechaBonita);
                    }
                },
                year, month, day
        );
        dialog.show();
    }

    private void cargarHistoricoDesdeServidor(int deviceId, String startDate, String endDate) {
        ApiService apiService = RetrofitClient
                .getRetrofitInstance()
                .create(ApiService.class);

        Call<List<MeasurementDto>> call =
                apiService.getMeasurementsFiltered(streetId, String.valueOf(deviceId), startDate, endDate);


        call.enqueue(new Callback<List<MeasurementDto>>() {
            @Override
            public void onResponse(Call<List<MeasurementDto>> call,
                                   Response<List<MeasurementDto>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    textLastState.setText("Error HTTP: " + response.code());
                    return;
                }

                List<MeasurementDto> lista = response.body();
                history.clear();
                history.addAll(lista);
                adapter.notifyDataSetChanged();

                if (!lista.isEmpty()) {
                    actualizarTextViewsConMedicion(lista.get(0), "Histórico BD");
                    recyclerHistory.scrollToPosition(0);
                } else {
                    textLastState.setText("No hay datos para el filtro seleccionado.");
                    textLastTimestamp.setText("");
                }
            }

            @Override
            public void onFailure(Call<List<MeasurementDto>> call, Throwable t) {
                textLastState.setText("Error de red: " + t.getMessage());
            }
        });
    }

    private void conectarMqtt(String topic) {
        try {
            client = new MqttClient(
                    MQTT_BROKER_URI,
                    MqttClient.generateClientId(),
                    new MemoryPersistence()
            );

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(5);

            Log.i("ubicua", "Conectando MQTT: " + MQTT_BROKER_URI);
            client.connect(options);
            Log.i("ubicua", "Conectado ✔");

            client.subscribe(topic, (receivedTopic, message) -> {
                String msg = new String(message.getPayload());
                Log.i("ubicua", "[" + receivedTopic + "] " + msg);

                try {
                    JSONObject root = new JSONObject(msg);
                    if (root.has("street_id")) {
                        String msgStreet = root.getString("street_id");
                        if (!streetId.equals(msgStreet)) {
                            Log.w("ubicua", "Mensaje ignorado: street_id=" + msgStreet + " pero esperábamos " + streetId);
                            return;
                        }
                    }

                    JSONObject data = root.getJSONObject("data");

                    MeasurementDto dto = new MeasurementDto();
                    dto.setTimestamp(root.getString("timestamp"));
                    dto.setCurrentState(data.getString("current_state"));
                    dto.setCyclePositionSeconds(data.getInt("cycle_position_seconds"));
                    dto.setTimeRemainingSeconds(data.getInt("time_remaining_seconds"));
                    dto.setCycleDurationSeconds(data.getInt("cycle_duration_seconds"));
                    dto.setTrafficLightType(data.getString("traffic_light_type"));
                    dto.setCirculationDirection(data.getString("circulation_direction"));
                    dto.setPedestrianWaiting(data.getBoolean("pedestrian_waiting"));
                    dto.setPedestrianButtonPressed(data.getBoolean("pedestrian_button_pressed"));
                    dto.setMalfunctionDetected(data.getBoolean("malfunction_detected"));
                    dto.setCycleCount(data.getInt("cycle_count"));
                    dto.setStateChanged(data.getBoolean("state_changed"));
                    dto.setLastStateChange(data.optString("last_state_change", ""));
                    dto.setDispositivoSensorId(Integer.valueOf(root.getString("sensor_id")));

                    // Seguridad: filtrar por deviceId
                    if (dto.getDispositivoSensorId() != null && dto.getDispositivoSensorId() != deviceId) {
                        Log.w("ubicua", "Mensaje MQTT ignorado: sensor_id=" + dto.getDispositivoSensorId()
                                + " pero esperábamos deviceId=" + deviceId);
                        return;
                    }

                    actualizarTextViewsConMedicion(dto, "MQTT");

                    runOnUiThread(() -> {
                        history.add(0, dto);
                        adapter.notifyItemInserted(0);
                        recyclerHistory.scrollToPosition(0);
                    });

                } catch (JSONException e) {
                    Log.e("ubicua", "Error parseando JSON MQTT", e);
                }
            });

            Log.i("ubicua", "Suscrito a: " + topic);

        } catch (MqttException e) {
            Log.e("ubicua", "Error MQTT: " + e.getMessage(), e);
            runOnUiThread(() -> textLastState.setText("Error MQTT: " + e.getMessage()));
        }
    }

    private void actualizarTextViewsConMedicion(MeasurementDto m, String origen) {
        String estado = m.getCurrentState();
        String ts     = TimeUtils.toMadrid(m.getTimestamp());
        Integer rem   = m.getTimeRemainingSeconds();

        runOnUiThread(() -> {
            textLastState.setText(
                    "Estado actual (" + origen + "): " + estado +
                            (rem != null ? "  •  T. restante: " + rem + " s" : "")
            );
            textLastTimestamp.setText("Última actualización: " + ts);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                Log.i("ubicua", "Desconectado MQTT");
            }
        } catch (MqttException e) {
            Log.e("ubicua", "Error al desconectar MQTT", e);
        }
    }
}
