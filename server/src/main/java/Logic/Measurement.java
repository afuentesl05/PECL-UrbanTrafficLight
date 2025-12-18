package logic;

import java.sql.Timestamp;

public class Measurement {

    // Campos que reflejan la tabla "informacion"
    private Timestamp timestamp;              // PK
    private String currentState;
    private Integer cyclePositionSeconds;
    private Integer timeRemainingSeconds;
    private Integer cycleDurationSeconds;
    private String trafficLightType;
    private String circulationDirection;
    private Boolean pedestrianWaiting;
    private Boolean pedestrianButtonPressed;
    private Boolean malfunctionDetected;
    private Integer cycleCount;
    private Boolean stateChanged;
    private String lastStateChange;           
    private Integer dispositivoSensorId;

    // Constructor por defecto
    public Measurement() {
        this.timestamp = null;
        this.currentState = null;
        this.cyclePositionSeconds = null;
        this.timeRemainingSeconds = null;
        this.cycleDurationSeconds = null;
        this.trafficLightType = null;
        this.circulationDirection = null;
        this.pedestrianWaiting = null;
        this.pedestrianButtonPressed = null;
        this.malfunctionDetected = null;
        this.cycleCount = null;
        this.stateChanged = null;
        this.lastStateChange = null;
        this.dispositivoSensorId = null;
    }

    
    public Measurement(
            Timestamp timestamp,
            String currentState,
            Integer cyclePositionSeconds,
            Integer timeRemainingSeconds,
            Integer cycleDurationSeconds,
            String trafficLightType,
            String circulationDirection,
            Boolean pedestrianWaiting,
            Boolean pedestrianButtonPressed,
            Boolean malfunctionDetected,
            Integer cycleCount,
            Boolean stateChanged,
            String lastStateChange,
            Integer dispositivoSensorId) {

        this.timestamp = timestamp;
        this.currentState = currentState;
        this.cyclePositionSeconds = cyclePositionSeconds;
        this.timeRemainingSeconds = timeRemainingSeconds;
        this.cycleDurationSeconds = cycleDurationSeconds;
        this.trafficLightType = trafficLightType;
        this.circulationDirection = circulationDirection;
        this.pedestrianWaiting = pedestrianWaiting;
        this.pedestrianButtonPressed = pedestrianButtonPressed;
        this.malfunctionDetected = malfunctionDetected;
        this.cycleCount = cycleCount;
        this.stateChanged = stateChanged;
        this.lastStateChange = lastStateChange;
        this.dispositivoSensorId = dispositivoSensorId;
    }

    // Getters y setters

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public Integer getCyclePositionSeconds() {
        return cyclePositionSeconds;
    }

    public void setCyclePositionSeconds(Integer cyclePositionSeconds) {
        this.cyclePositionSeconds = cyclePositionSeconds;
    }

    public Integer getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public void setTimeRemainingSeconds(Integer timeRemainingSeconds) {
        this.timeRemainingSeconds = timeRemainingSeconds;
    }

    public Integer getCycleDurationSeconds() {
        return cycleDurationSeconds;
    }

    public void setCycleDurationSeconds(Integer cycleDurationSeconds) {
        this.cycleDurationSeconds = cycleDurationSeconds;
    }

    public String getTrafficLightType() {
        return trafficLightType;
    }

    public void setTrafficLightType(String trafficLightType) {
        this.trafficLightType = trafficLightType;
    }

    public String getCirculationDirection() {
        return circulationDirection;
    }

    public void setCirculationDirection(String circulationDirection) {
        this.circulationDirection = circulationDirection;
    }

    public Boolean getPedestrianWaiting() {
        return pedestrianWaiting;
    }

    public void setPedestrianWaiting(Boolean pedestrianWaiting) {
        this.pedestrianWaiting = pedestrianWaiting;
    }

    public Boolean getPedestrianButtonPressed() {
        return pedestrianButtonPressed;
    }

    public void setPedestrianButtonPressed(Boolean pedestrianButtonPressed) {
        this.pedestrianButtonPressed = pedestrianButtonPressed;
    }

    public Boolean getMalfunctionDetected() {
        return malfunctionDetected;
    }

    public void setMalfunctionDetected(Boolean malfunctionDetected) {
        this.malfunctionDetected = malfunctionDetected;
    }

    public Integer getCycleCount() {
        return cycleCount;
    }

    public void setCycleCount(Integer cycleCount) {
        this.cycleCount = cycleCount;
    }

    public Boolean getStateChanged() {
        return stateChanged;
    }

    public void setStateChanged(Boolean stateChanged) {
        this.stateChanged = stateChanged;
    }

    public String getLastStateChange() {
        return lastStateChange;
    }

    public void setLastStateChange(String lastStateChange) {
        this.lastStateChange = lastStateChange;
    }

    public Integer getDispositivoSensorId() {
        return dispositivoSensorId;
    }

    public void setDispositivoSensorId(Integer dispositivoSensorId) {
        this.dispositivoSensorId = dispositivoSensorId;
    }
}
