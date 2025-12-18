package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import logic.Log;
import mqtt.MQTTBroker;
import mqtt.MQTTPublisher;

@WebServlet("/SetData")
public class SetData extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Si no se pasa streetId, usamos el de prácticas por defecto
    private static final String DEFAULT_STREET_ID = "ST_2245";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // DEBUG útil para ver qué llega realmente
        Log.log.info("[SetData] query=" + request.getQueryString());

        String action = request.getParameter("action");
        if (action == null || action.isBlank()) action = "force";

        String deviceIdParam = request.getParameter("deviceId");
        String streetId = request.getParameter("streetId");
        if (streetId == null || streetId.isBlank()) streetId = DEFAULT_STREET_ID;

        if (deviceIdParam == null || deviceIdParam.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("ERROR: deviceId requerido");
            Log.log.warn("[SetData] Falta deviceId");
            return;
        }

        final int deviceId;
        try {
            deviceId = Integer.parseInt(deviceIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("ERROR: deviceId invalido (" + deviceIdParam + ")");
            Log.log.warn("[SetData] deviceId inválido: " + deviceIdParam);
            return;
        }

        // Topic dinámico por streetId + deviceId
        String cmdTopic = String.format(
                "sensors/%s/traffic_light/TL_%03d/cmd",
                streetId, deviceId
        );

        try {
            MQTTBroker broker = new MQTTBroker();
            String jsonCmd;

            switch (action) {
                case "buzzer": {
                    String enabledParam = request.getParameter("enabled");
                    boolean enabled = "true".equalsIgnoreCase(enabledParam);

                    jsonCmd = "{ \"buzzer\": " + (enabled ? "true" : "false") + " }";
                    MQTTPublisher.publish(broker, cmdTopic, jsonCmd);

                    out.println("OK buzzer -> topic=" + cmdTopic);
                    Log.log.info("[SetData] buzzer=" + enabled + " topic=" + cmdTopic + " payload=" + jsonCmd);
                    break;
                }

                case "force":
                default: {
                    jsonCmd = "{ \"force\": \"ped_green\" }";
                    MQTTPublisher.publish(broker, cmdTopic, jsonCmd);

                    out.println("OK force -> topic=" + cmdTopic);
                    Log.log.info("[SetData] force topic=" + cmdTopic + " payload=" + jsonCmd);
                    break;
                }
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("ERROR: exception enviando MQTT");
            Log.log.error("[SetData] Exception:", e);
        } finally {
            out.close();
        }
    }
}
