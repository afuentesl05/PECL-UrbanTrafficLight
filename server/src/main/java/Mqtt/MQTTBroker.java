package mqtt;

public class MQTTBroker {

    private static int qos = 2;
    private static final String broker   = "tcp://mqtt-broker:1883"; // <- nombre del servicio Docker
    private static final String clientId = "SemaforoAyL";
    private static final String username = "";
    private static final String password = "";

    public MQTTBroker() {}

    public static int getQos() {
        return qos;
    }

    public static String getBroker() {
        return broker;
    }

    public static String getClientId() {
        return clientId;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }

    public static String getSubscriberClientId() {
        return clientId + "-subscriber";
    }

    public static String getPublisherClientId() {
        return clientId + "-publisher";
    }
}
