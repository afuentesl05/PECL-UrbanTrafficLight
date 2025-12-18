package logic;

import Database.ConectionDDBB;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Logic {

    /**
     * Obtiene todos los registros de la tabla "informacion" y los mapea a
     * objetos Measurement.
     */
    public static ArrayList<Measurement> getDataFromDB() {
        ArrayList<Measurement> values = new ArrayList<>();

        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            Log.log.info("Database Connected");

            PreparedStatement ps = ConectionDDBB.GetDataBD(con);
            Log.log.info("Query => " + ps.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Measurement m = new Measurement();

                // Campos directos de la tabla "informacion"
                m.setTimestamp(rs.getTimestamp("timestamp"));
                m.setCurrentState(rs.getString("current_state"));
                m.setCyclePositionSeconds(getNullableInt(rs, "cycle_position_seconds"));
                m.setTimeRemainingSeconds(getNullableInt(rs, "time_remaining_seconds"));
                m.setCycleDurationSeconds(getNullableInt(rs, "cycle_duration_seconds"));
                m.setTrafficLightType(rs.getString("traffic_light_type"));
                m.setCirculationDirection(rs.getString("circulation_direction"));

                // Flags CHAR(1) -> Boolean
                m.setPedestrianWaiting(charToBoolean(rs.getString("pedestrian_waiting")));
                m.setPedestrianButtonPressed(charToBoolean(rs.getString("pedestrian_button_pressed")));
                m.setMalfunctionDetected(charToBoolean(rs.getString("malfunction_detected")));
                m.setCycleCount(getNullableInt(rs, "cycle_count"));
                m.setStateChanged(charToBoolean(rs.getString("state_changed")));
                m.setLastStateChange(rs.getString("last_state_change"));
                m.setDispositivoSensorId(getNullableInt(rs, "dispositivo_sensor_id"));

                values.add(m);
            }
        } catch (SQLException e) {
            Log.log.error("Error SQL en getDataFromDB: " + e);
            values = new ArrayList<>();
        } catch (NullPointerException e) {
            Log.log.error("NullPointer en getDataFromDB: " + e);
            values = new ArrayList<>();
        } catch (Exception e) {
            Log.log.error("Error genérico en getDataFromDB: " + e);
            values = new ArrayList<>();
        } finally {
            conector.closeConnection(con);
        }

        return values;
    }

    /**
     * Método heredado del ejemplo original. Con el nuevo esquema de la tabla
     * "informacion" ya no tiene sentido insertar solo un entero, así que por ahora
     * NO realiza inserciones y solo se mantiene para que el servlet SetData compile.
     */
    public static ArrayList<Measurement> setDataToDB(int value) {
        Log.log.warn("setDataToDB(int) ya no se usa con el esquema actual de 'informacion'. No se realiza ninguna inserción.");
        return new ArrayList<>();
    }

    /**
     * Inserta en la tabla "informacion" los datos recibidos en formato JSON
     * desde MQTT (payload publicado por el ESP32).
     */
    public static void insertFromMqttJson(String payload) {
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;

        try {
            con = conector.obtainConnection(true);
            Log.logdb.info("Database Connected (insertFromMqttJson)");

            JsonObject root = JsonParser.parseString(payload).getAsJsonObject();

            // --- datos del JSON ---
            String sensorIdStr = root.get("sensor_id").getAsString();      // "1"
       

            String timestampStr = root.get("timestamp").getAsString();     // "2025-11-27T11:22:33Z"
            Instant instant = Instant.parse(timestampStr);
            Timestamp ts = Timestamp.from(instant);

            JsonObject data = root.getAsJsonObject("data");

            String currentState         = data.get("current_state").getAsString();
            int cyclePosSec             = data.get("cycle_position_seconds").getAsInt();
            int timeRemSec              = data.get("time_remaining_seconds").getAsInt();
            int cycleDurSec             = data.get("cycle_duration_seconds").getAsInt();
            String trafficLightType     = data.get("traffic_light_type").getAsString();
            String circulationDirection = data.get("circulation_direction").getAsString();
            boolean pedWaiting          = data.get("pedestrian_waiting").getAsBoolean();
            boolean pedButton           = data.get("pedestrian_button_pressed").getAsBoolean();
            boolean malfunction         = data.get("malfunction_detected").getAsBoolean();
            int cycleCount              = data.get("cycle_count").getAsInt();
            boolean stateChanged        = data.get("state_changed").getAsBoolean();
            String lastStateChangeStr   = data.get("last_state_change").getAsString(); 

            // --- mapeos a tipos de la tabla ---
            // CHAR(1) lo guardamos como '1' (true) o '0' (false)
            String pedWaitingChar   = pedWaiting   ? "1" : "0";
            String pedButtonChar    = pedButton    ? "1" : "0";
            String malfunctionChar  = malfunction  ? "1" : "0";
            String stateChangedChar = stateChanged ? "1" : "0";

            // last_state_change en tu tabla es CHAR(1); guardamos simplemente '1' si tenemos fecha
            String lastStateChangeChar = (lastStateChangeStr != null && !lastStateChangeStr.isEmpty()) ? "1" : "0";

            // dispositivo_sensor_id: suponemos que sensor_id del JSON es un número tipo "1"
            int dispositivoSensorId = Integer.parseInt(sensorIdStr);

            try {
                Database.DeviceDAO.ensureDeviceExists(con, dispositivoSensorId);
            } catch (SQLException e) {
                // Si falla la creación, lo registramos pero seguimos intentando insertar la medida
                Log.logdb.error("Error ensuring device " + dispositivoSensorId + " exists in `dispositivo`", e);
            }

            // === Inserción en `informacion` como hasta ahora ===
            PreparedStatement ps = ConectionDDBB.SetDataBD(con);
            int idx = 1;
            ps.setTimestamp(idx++, ts);
            ps.setString(idx++, currentState);
            ps.setInt(idx++, cyclePosSec);
            ps.setInt(idx++, timeRemSec);
            ps.setInt(idx++, cycleDurSec);
            ps.setString(idx++, trafficLightType);
            ps.setString(idx++, circulationDirection);
            ps.setString(idx++, pedWaitingChar);
            ps.setString(idx++, pedButtonChar);
            ps.setString(idx++, malfunctionChar);
            ps.setInt(idx++, cycleCount);
            ps.setString(idx++, stateChangedChar);
            ps.setString(idx++, lastStateChangeChar);
            ps.setInt(idx++, dispositivoSensorId);

            Log.logdb.info("Query => " + ps.toString());
            ps.executeUpdate();


        } catch (Exception e) {
            Log.logdb.error("Error inserting from MQTT JSON", e);
        } finally {
            conector.closeConnection(con);
        }
    }
    

    // ===================== HELPERS PRIVADOS (para no liarla) =====================

    private static boolean tryFillDistinctStrings(Connection con, String sql, ArrayList<String> out) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = ConectionDDBB.getStatement(con, sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                String s = rs.getString(1);
                if (s != null && !s.isBlank()) out.add(s);
            }
            return true;
        } catch (Exception ex) {
            Log.log.warn("Query fallida (Strings): " + sql + " -> " + ex.getMessage());
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }
    }

    private static boolean tryFillDistinctInts(Connection con, String sql, ArrayList<Integer> out, String param1) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = ConectionDDBB.getStatement(con, sql);
            ps.setString(1, param1);
            rs = ps.executeQuery();
            while (rs.next()) {
                int v = rs.getInt(1);
                if (!rs.wasNull()) out.add(v);
            }
            return true;
        } catch (Exception ex) {
            Log.log.warn("Query fallida (Ints): " + sql + " -> " + ex.getMessage());
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        }
    }


    // ===================== Métodos auxiliares privados =====================

    /**
     * Devuelve un Integer a partir de una columna INT que puede ser NULL.
     */
    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    /**
     * Convierte un CHAR(1) ('1'/'0', 'Y'/'N', etc.) a Boolean.
     */
    private static Boolean charToBoolean(String c) {
        if (c == null) {
            return null;
        }
        c = c.trim().toUpperCase();
        if (c.isEmpty()) {
            return null;
        }
        // True si es '1', 'Y', 'T', 'S'...
        return c.equals("1") || c.equals("Y") || c.equals("T") || c.equals("S");
    }
    // ===================== NUEVOS: calles y dispositivos =====================

    public static ArrayList<String> getStreetsFromDB() {
        ArrayList<String> streets = new ArrayList<>();
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;

        try {
            con = conector.obtainConnection(true);
            String sql = "SELECT street_id FROM calle ORDER BY street_id";
            PreparedStatement ps = ConectionDDBB.getStatement(con, sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) streets.add(rs.getString("street_id"));
        } catch (Exception e) {
            Log.log.error("Error en getStreetsFromDB: " + e, e);
            streets = new ArrayList<>();
        } finally {
            conector.closeConnection(con);
        }
        return streets;
    }

    public static ArrayList<Integer> getDevicesByStreetFromDB(String streetId) {
        ArrayList<Integer> devices = new ArrayList<>();
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;

        try {
            con = conector.obtainConnection(true);
            String sql = "SELECT sensor_id FROM dispositivo WHERE calle_street_id = ? ORDER BY sensor_id";
            PreparedStatement ps = ConectionDDBB.getStatement(con, sql);
            ps.setString(1, streetId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) devices.add(rs.getInt("sensor_id"));
        } catch (Exception e) {
            Log.log.error("Error en getDevicesByStreetFromDB: " + e, e);
            devices = new ArrayList<>();
        } finally {
            conector.closeConnection(con);
        }
        return devices;
    }

    // ===================== FILTRADO: ahora con streetId =====================

    // Compatibilidad: el viejo método sigue existiendo
    public static ArrayList<Measurement> getDataFromDBFiltered(
            String deviceParam,
            String startParam,
            String endParam) {
        return getDataFromDBFiltered(null, deviceParam, startParam, endParam);
    }

    public static ArrayList<Measurement> getDataFromDBFiltered(
            String streetId,
            String deviceParam,
            String startParam,
            String endParam) {

        ArrayList<Measurement> values = new ArrayList<>();
        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;

        try {
            con = conector.obtainConnection(true);
            Log.log.info("Database Connected (filtered street+device+dates)");

            StringBuilder sql = new StringBuilder(
                    "SELECT i.* " +
                    "FROM informacion i " +
                    "JOIN dispositivo d ON d.sensor_id = i.dispositivo_sensor_id " +
                    "WHERE 1=1"
            );

            ArrayList<Object> params = new ArrayList<>();

            // Filtro por calle
            if (streetId != null && !streetId.isBlank()) {
                sql.append(" AND d.calle_street_id = ?");
                params.add(streetId);
            }

            // Filtro por dispositivo
            if (deviceParam != null && !deviceParam.isBlank()
                    && !"all".equalsIgnoreCase(deviceParam)) {
                sql.append(" AND i.dispositivo_sensor_id = ?");
                params.add(Integer.parseInt(deviceParam));
            }

            // Filtro por fecha inicio
            if (startParam != null && !startParam.isBlank()) {
                sql.append(" AND i.timestamp >= ?");
                params.add(parseTimestampParam(startParam));
            }

            // Filtro por fecha fin
            if (endParam != null && !endParam.isBlank()) {
                sql.append(" AND i.timestamp <= ?");
                params.add(parseTimestampParam(endParam));
            }

            sql.append(" ORDER BY i.timestamp DESC");

            PreparedStatement ps = ConectionDDBB.getStatement(con, sql.toString());
            Log.log.info("Filtered Query => " + sql);

            int idx = 1;
            for (Object p : params) {
                if (p instanceof Integer) {
                    ps.setInt(idx++, (Integer) p);
                } else if (p instanceof String) {
                    ps.setString(idx++, (String) p);
                } else if (p instanceof Timestamp) {
                    ps.setTimestamp(idx++, (Timestamp) p);
                }
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Measurement m = new Measurement();
                m.setTimestamp(rs.getTimestamp("timestamp"));
                m.setCurrentState(rs.getString("current_state"));
                m.setCyclePositionSeconds(getNullableInt(rs, "cycle_position_seconds"));
                m.setTimeRemainingSeconds(getNullableInt(rs, "time_remaining_seconds"));
                m.setCycleDurationSeconds(getNullableInt(rs, "cycle_duration_seconds"));
                m.setTrafficLightType(rs.getString("traffic_light_type"));
                m.setCirculationDirection(rs.getString("circulation_direction"));
                m.setPedestrianWaiting(charToBoolean(rs.getString("pedestrian_waiting")));
                m.setPedestrianButtonPressed(charToBoolean(rs.getString("pedestrian_button_pressed")));
                m.setMalfunctionDetected(charToBoolean(rs.getString("malfunction_detected")));
                m.setCycleCount(getNullableInt(rs, "cycle_count"));
                m.setStateChanged(charToBoolean(rs.getString("state_changed")));
                m.setLastStateChange(rs.getString("last_state_change"));
                m.setDispositivoSensorId(getNullableInt(rs, "dispositivo_sensor_id"));
                values.add(m);
            }

        } catch (Exception e) {
            Log.log.error("Error en getDataFromDBFiltered(streetId,...): " + e, e);
            values = new ArrayList<>();
        } finally {
            conector.closeConnection(con);
        }

        return values;
    }

    /**
     * Acepta:
     *  - "yyyy-MM-ddTHH:mm"
     *  - "yyyy-MM-ddTHH:mm:ss"
     *  - o ya con espacio en vez de T
     */
    private static Timestamp parseTimestampParam(String param) {
        String s = param.trim().replace("T", " ");
        if (s.length() == 16) s += ":00"; // añade segundos si faltan
        return Timestamp.valueOf(s);
    }




}
