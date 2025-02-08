CREATE TABLE IF NOT EXISTS station
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    main_qrz VARCHAR(20) NOT NULL UNIQUE,
    description TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS agent
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    station_id UUID NOT NULL,
    display_name VARCHAR(30) NOT NULL,
    secret VARCHAR(256),
    UNIQUE (station_id, display_name)
)