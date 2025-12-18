-- Esquema PECL2 - MariaDB

CREATE TABLE calle (
    street_id    VARCHAR(30) NOT NULL,
    latitud      DECIMAL(9,6),
    longitud     DECIMAL(9,6),
    district     VARCHAR(30),
    neighborhood VARCHAR(30),
    sensor_id    INT NOT NULL,
    PRIMARY KEY (street_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO calle (street_id, latitud, longitud, district, neighborhood, sensor_id)
VALUES ('ST_2245', 40.417000, -3.703000, 'Moncloa-Aravaca', 'Moncloa-Aravaca', 1);

CREATE TABLE dispositivo (
    sensor_id       INT NOT NULL,
    sensor_type     VARCHAR(50),
    calle_street_id VARCHAR(30) NOT NULL,
    PRIMARY KEY (sensor_id),
    CONSTRAINT dispositivo_calle_fk
      FOREIGN KEY (calle_street_id)
      REFERENCES calle (street_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO dispositivo (sensor_id, sensor_type, calle_street_id)
VALUES (1, 'traffic_light', 'ST_2245');

CREATE TABLE informacion (
    `timestamp`              DATETIME NOT NULL,
    current_state            VARCHAR(30),
    cycle_position_seconds   INT,
    time_remaining_seconds   INT,
    cycle_duration_seconds   INT,
    traffic_light_type       VARCHAR(50),
    circulation_direction    VARCHAR(30),
    pedestrian_waiting       CHAR(1),
    pedestrian_button_pressed CHAR(1),
    malfunction_detected     CHAR(1),
    cycle_count              INT,
    state_changed            CHAR(1),
    last_state_change        CHAR(1),
    dispositivo_sensor_id    INT NOT NULL,
    PRIMARY KEY (`timestamp`),
    CONSTRAINT informacion_dispositivo_fk
      FOREIGN KEY (dispositivo_sensor_id)
      REFERENCES dispositivo (sensor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
