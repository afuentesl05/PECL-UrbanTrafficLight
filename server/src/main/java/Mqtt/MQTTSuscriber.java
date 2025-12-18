package mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import Database.Topics;
import logic.Log;
import logic.Logic;

public class MQTTSuscriber implements MqttCallback {

    private MqttClient client;
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;

    public MQTTSuscriber(MQTTBroker broker) {
        this.brokerUrl = broker.getBroker();
        this.clientId = broker.getClientId();
        this.username = broker.getUsername();
        this.password = broker.getPassword();
    }

    public void subscribeTopic(String topic) {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, MQTTBroker.getSubscriberClientId(), persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setCleanSession(false);          // mantener suscripción
            connOpts.setAutomaticReconnect(true);     // reconexión automática
            connOpts.setConnectionTimeout(10);

            client.setCallback(this);
            client.connect(connOpts);

            client.subscribe(topic, 1); // QoS 1
            Log.logmqtt.info("Subscribed to {}", topic);

        } catch (MqttException e) {
            Log.logmqtt.error("Error subscribing to topic: {}", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.logmqtt.warn("MQTT Connection lost, cause: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = message.toString();
        Log.logmqtt.info("Message arrived. Topic: {} Payload: {}", topic, payload);

        // Guardamos el último valor en un objeto Topics (por si quieres usarlo después)
        Topics newTopic = new Topics();
        newTopic.setIdTopic(topic);
        newTopic.setValue(payload);

        try {
            // Inserta en la tabla "informacion" usando el JSON
            Logic.insertFromMqttJson(payload);
        } catch (Exception e) {
            Log.logmqtt.error("Error processing MQTT message and inserting into DB", e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No usamos publicación desde el subscriber
    }
}
