# QSP Signaling Server

QSP Signaling Server is the signaling component of QSP, a project for operating
amateur radio transceivers remotely over an Internet or local network
connection.

The signaling server connects QSP clients and QSP agents together. Agents keep a
WebSocket session open with the server, clients discover available sessions, and
the server forwards the signaling data required to establish the remote
connection.

## Docker Image

```shell
docker pull f4fez/qsp-signaling-server:latest
```

Release tags are also published when project releases are created.

## Requirements

- PostgreSQL 17 or newer
- An OAuth2 / OpenID Connect issuer that can provide JWT tokens accepted by the
  Spring resource server
- Docker or another OCI-compatible container runtime

## Quick Start

Create the PostgreSQL database and initialize the schema from the project SQL
file:

```sql
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
);
```

Then start the container:

```shell
docker run --rm \
  --name qsp-signaling-server \
  -p 7080:7080 \
  -e SPRING_R2DBC_URL=r2dbc:postgresql://host.docker.internal:5432/qsp \
  -e SPRING_R2DBC_USERNAME=qsp \
  -e SPRING_R2DBC_PASSWORD=qsp_password \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://issuer.example.org \
  -e QSP_SERVERNAME="QSP signaling server" \
  -e QSP_SERVERDESCRIPTION="Public QSP signaling service" \
  f4fez/qsp-signaling-server:latest
```

If PostgreSQL is running in another Docker container, place both containers on
the same Docker network and replace `host.docker.internal` with the PostgreSQL
container name.

## Configuration

The image uses the standard Spring Boot environment variable mapping. The most
important settings are:

| Variable | Description | Default |
| --- | --- | --- |
| `SERVER_PORT` | HTTP and WebSocket listen port inside the container. | `7080` |
| `SPRING_R2DBC_URL` | PostgreSQL R2DBC connection URL. | `r2dbc:postgresql://localhost:5432/postgres` |
| `SPRING_R2DBC_USERNAME` | PostgreSQL username. | `postgres` |
| `SPRING_R2DBC_PASSWORD` | PostgreSQL password. | `pgpass` |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | OAuth2 / OpenID Connect issuer URI used to validate JWT tokens. | `http://localhost:9000` |
| `QSP_SERVERNAME` | Name returned in the server description payload. | `Local dev server` |
| `QSP_SERVERDESCRIPTION` | Description returned in the server description payload. | `Local execution of the signaling server` |

## Main Endpoints

| Endpoint | Description |
| --- | --- |
| `GET /server/description` | Returns the server description. Requires authentication. |
| `WS /server/session` | Agent WebSocket endpoint. |
| `GET /client/agents` | Lists connected agent sessions. Requires authentication. |
| `POST /client/signal` | Sends a client signaling request to an agent session. Requires authentication. |
| `/station` | Station and agent management API. Requires authentication. |

## Project Links

- Source code: https://github.com/FEZ-Remote/fez-remote-signal-server
- Docker image: https://hub.docker.com/r/f4fez/qsp-signaling-server
- License: GPL-3.0-or-later
