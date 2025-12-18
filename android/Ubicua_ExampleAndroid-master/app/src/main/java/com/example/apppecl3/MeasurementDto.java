package com.example.apppecl3;

public class MeasurementDto {
    private String timestamp;
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
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
