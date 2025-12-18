package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import logic.Log;

public class DeviceDAO {

    
    private static final String DEFAULT_SENSOR_TYPE = "traffic_light";
    private static final String DEFAULT_CALLE_STREET_ID = "ST_2245";

    /**
     * Garantiza que exista un registro en la tabla `dispositivo`.
     * Si no existe, crea automáticamente el dispositivo con valores por defecto.
     *
     * @param con        conexión abierta (NO se cierra aquí)
     * @param sensorId   sensor_id del mensaje MQTT
     */
    public static void ensureDeviceExists(Connection con, int sensorId) throws SQLException {

        // 1) Comprobar si existe en la tabla dispositivo
        String checkSql = "SELECT sensor_id FROM dispositivo WHERE sensor_id = ?";
        try (PreparedStatement ps = con.prepareStatement(checkSql)) {
            ps.setInt(1, sensorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Ya existe
                    Log.logdb.debug("Device " + sensorId + " already exists in `dispositivo`");
                    return;
                }
            }
        }

        Log.logdb.info("Device " + sensorId + " does not exist. Creating it in `dispositivo` ...");

        // 2) Insertar un nuevo dispositivo con valores por defecto
        String insertSql =
            "INSERT INTO dispositivo (" +
            "sensor_id, sensor_type, calle_street_id" +
            ") VALUES (?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
            ps.setInt(1, sensorId);
            ps.setString(2, DEFAULT_SENSOR_TYPE);
            ps.setString(3, DEFAULT_CALLE_STREET_ID);
            ps.executeUpdate();
        }

        Log.logdb.info("Device " + sensorId + " created successfully in `dispositivo`");
    }
}
