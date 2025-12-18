package com.example.apppecl3;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttConsoleActivity extends AppCompatActivity {

    private static final String TAG = "MqttConsoleActivity";
    private static final String MQTT_BROKER_URI = "tcp://192.168.0.73:1883"; //IP DEL BROKER

    private MqttClient client;

    private EditText editSubTopic;
    private EditText editPubTopic;
    private EditText editPubPayload;
    private Button btnAddSub;
    private Button btnPublish;
    private Button btnClearLog;
    private TextView textLog;

    private final StringBuilder logBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mqtt_console);
        StrangerFx.attachIfEnabled(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Referencias UI
        editSubTopic   = findViewById(R.id.editSubTopic);
        editPubTopic   = findViewById(R.id.editPubTopic);
        editPubPayload = findViewById(R.id.editPubPayload);
        btnAddSub      = findViewById(R.id.btnAddSub);
        btnPublish     = findViewById(R.id.btnPublish);
        btnClearLog    = findViewById(R.id.btnClearLog);
        textLog        = findViewById(R.id.textLog);

        textLog.setMovementMethod(new ScrollingMovementMethod());

        // Animaciones en botones
        UiUtils.setAnimatedClick(btnAddSub, this::handleAddSubscription);
        UiUtils.setAnimatedClick(btnPublish, this::handlePublish);
        UiUtils.setAnimatedClick(btnClearLog, this::clearLog);

        // Conectar a MQTT
        new Thread(this::connectMqtt).start();
    }

    private void connectMqtt() {
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

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    addLog("Conexión MQTT perdida: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    addLog("[RX] " + topic + " -> " + payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Opcional: log de envíos completados
                }
            });

            addLog("Conectando a " + MQTT_BROKER_URI + " ...");
            client.connect(options);
            addLog("Conectado a MQTT");

        } catch (MqttException e) {
            Log.e(TAG, "Error conectando a MQTT", e);
            addLog("Error conectando: " + e.getMessage());
        }
    }

    // ===================== Acciones de UI =====================

    private void handleAddSubscription() {
        String topic = editSubTopic.getText().toString().trim();
        if (topic.isEmpty()) {
            Toast.makeText(this, "Introduce un topic para suscribirte", Toast.LENGTH_SHORT).show();
            return;
        }

        if (client == null || !client.isConnected()) {
            Toast.makeText(this, "MQTT no conectado todavía", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            client.subscribe(topic, 1);
            addLog("Suscrito a topic: " + topic);
            Toast.makeText(this, "Suscrito a " + topic, Toast.LENGTH_SHORT).show();
        } catch (MqttException e) {
            Log.e(TAG, "Error suscribiendo a " + topic, e);
            addLog("Error suscribiendo a " + topic + ": " + e.getMessage());
        }
    }

    private void handlePublish() {
        String topic   = editPubTopic.getText().toString().trim();
        String payload = editPubPayload.getText().toString().trim();

        if (topic.isEmpty()) {
            Toast.makeText(this, "Introduce un topic para publicar", Toast.LENGTH_SHORT).show();
            return;
        }
        if (payload.isEmpty()) {
            Toast.makeText(this, "Introduce un contenido para publicar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (client == null || !client.isConnected()) {
            Toast.makeText(this, "MQTT no conectado todavía", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(1);
            msg.setRetained(false);

            client.publish(topic, msg);
            addLog("[TX] " + topic + " <- " + payload);
            Toast.makeText(this, "Publicado en " + topic, Toast.LENGTH_SHORT).show();
        } catch (MqttException e) {
            Log.e(TAG, "Error publicando en " + topic, e);
            addLog("Error publicando en " + topic + ": " + e.getMessage());
        }
    }

    private void clearLog() {
        logBuilder.setLength(0);
        runOnUiThread(() -> textLog.setText(""));
    }

    private void addLog(String line) {
        Log.d(TAG, line);
        logBuilder.append(line).append("\n");
        runOnUiThread(() -> {
            textLog.setText(logBuilder.toString());
            // Scroll al final
            int offset = textLog.getLayout() != null
                    ? textLog.getLayout().getLineTop(textLog.getLineCount()) - textLog.getHeight()
                    : 0;
            if (offset > 0) {
                textLog.scrollTo(0, offset);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                addLog("Desconectado de MQTT");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error al desconectar MQTT", e);
        }
    }
}
