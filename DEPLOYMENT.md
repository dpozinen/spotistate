# Spotistate Deployment Guide

## Docker Deployment

### Prerequisites

1. Docker and Docker Compose installed
2. Spotify Developer App configured with redirect URI
3. Environment variables set up

### Quick Start

1. **Download the docker-compose file:**
   ```bash
   curl -O https://raw.githubusercontent.com/dpozinen/spotistate/main/docker-compose.production.yml
   ```

2. **Set up environment variables:**
   ```bash
   export SPOTIFY_CLIENT_ID=your_client_id_here
   export SPOTIFY_CLIENT_SECRET=your_client_secret_here
   export POSTGRES_PASSWORD=your_secure_password_here
   ```

3. **Run the application:**
   ```bash
   docker-compose -f docker-compose.production.yml up -d
   ```

4. **Access the application:**
   - Frontend: http://localhost
   - API: http://localhost:8080
   - Health check: http://localhost/health

### Environment Variables

All Spring Boot properties can be overridden using environment variables. Here's the complete list:

#### Required Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SPOTIFY_CLIENT_ID` | Your Spotify app client ID | **Yes** | - |
| `SPOTIFY_CLIENT_SECRET` | Your Spotify app client secret | **Yes** | - |
| `POSTGRES_PASSWORD` | PostgreSQL password | **Yes** | - |

#### Application Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring Boot profile | `production` |
| `SERVER_PORT` | Application server port | `8080` |
| `APP_PORT` | External port for frontend | `80` |
| `API_PORT` | External port for API | `8080` |

#### Database Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_USER` | PostgreSQL username | `postgres` |
| `POSTGRES_DB` | PostgreSQL database name | `spotistate` |
| `POSTGRES_PORT` | PostgreSQL external port | `5432` |
| `SPRING_DATASOURCE_URL` | Database connection URL | `jdbc:postgresql://postgres:5432/spotistate` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Uses `POSTGRES_PASSWORD` |

#### JPA/Hibernate Configuration

| Variable | Description | Default | Options |
|----------|-------------|---------|---------|
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Database schema handling | `validate` | `create`, `create-drop`, `update`, `validate`, `none` |
| `SPRING_JPA_SHOW_SQL` | Show SQL in logs | `false` | `true`, `false` |
| `SPRING_JPA_HIBERNATE_FORMAT_SQL` | Format SQL in logs | `true` | `true`, `false` |

#### Logging Configuration

| Variable | Description | Default | Options |
|----------|-------------|---------|---------|
| `LOG_LEVEL_ROOT` | Root log level | `WARN` | `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE` |
| `LOG_LEVEL_SPOTISTATE` | Application log level | `INFO` | `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE` |
| `LOG_LEVEL_SECURITY` | Spring Security log level | `WARN` | `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE` |
| `LOG_LEVEL_WEB` | Spring Web log level | `WARN` | `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE` |
| `LOG_LEVEL_SQL` | Hibernate SQL log level | `WARN` | `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE` |
| `LOGGING_FILE_NAME` | Log file location | `/app/logs/spotistate.log` | Any valid path |
| `LOGGING_PATTERN_CONSOLE` | Console log pattern | Default Spring pattern | Any valid pattern |
| `LOGGING_PATTERN_FILE` | File log pattern | Default Spring pattern | Any valid pattern |

#### Management/Actuator Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `MANAGEMENT_ENDPOINTS_INCLUDE` | Exposed actuator endpoints | `health,info,metrics` |
| `MANAGEMENT_HEALTH_SHOW_DETAILS` | Health details visibility | `never` |
| `MANAGEMENT_HEALTH_DISKSPACE_ENABLED` | Enable disk space check | `true` |
| `MANAGEMENT_HEALTH_DB_ENABLED` | Enable database health check | `true` |
| `HEALTH_DISKSPACE_THRESHOLD` | Disk space warning threshold | `10MB` |

#### Spotify OAuth Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `SPOTIFY_REDIRECT_URI` | OAuth redirect URI | `http://127.0.0.1:8080/api/auth/callback` |

#### Application Metadata

| Variable | Description | Default |
|----------|-------------|---------|
| `APP_VERSION` | Application version | `latest` |
| `APP_ENCODING` | Application encoding | `UTF-8` |

### Using Environment Variables

You can override any of these variables in several ways:

1. **In docker-compose.yml:**
   ```yaml
   environment:
     - LOG_LEVEL_SPOTISTATE=DEBUG
     - SPRING_JPA_SHOW_SQL=true
   ```

2. **Using .env file:**
   ```bash
   LOG_LEVEL_SPOTISTATE=DEBUG
   SPRING_JPA_SHOW_SQL=true
   ```

3. **Command line:**
   ```bash
   LOG_LEVEL_SPOTISTATE=DEBUG docker-compose up
   ```

4. **Export variables:**
   ```bash
   export LOG_LEVEL_SPOTISTATE=DEBUG
   docker-compose up
   ```

### Spotify App Configuration

Make sure your Spotify app has the following redirect URI configured:
- `http://your-domain.com/api/auth/callback` (replace with your actual domain)
- For local development: `http://localhost:8080/api/auth/callback`

### Monitoring

- **Health checks**: The application includes health checks for both services
- **Logs**: View logs with `docker-compose logs -f spotistate`
- **Database**: Access with `docker-compose exec postgres psql -U postgres -d spotistate`

### Updating

To update to the latest version:

```bash
docker-compose -f docker-compose.production.yml pull
docker-compose -f docker-compose.production.yml up -d
```

### Backup

Backup your database:

```bash
docker-compose exec postgres pg_dump -U postgres spotistate > backup.sql
```

Restore from backup:

```bash
cat backup.sql | docker-compose exec -T postgres psql -U postgres spotistate
```

### Troubleshooting

1. **Container won't start**: Check logs with `docker-compose logs`
2. **Database connection issues**: Ensure PostgreSQL is healthy
3. **Spotify auth issues**: Verify client ID/secret and redirect URIs
4. **Port conflicts**: Change port mappings in docker-compose.yml

### Security Considerations

1. Use strong passwords for PostgreSQL
2. Keep Spotify credentials secure
3. Use HTTPS in production
4. Regularly update Docker images
5. Consider using Docker secrets for sensitive data

### Production Recommendations

1. **Reverse Proxy**: Use nginx or Traefik for HTTPS termination
2. **Monitoring**: Add Prometheus/Grafana for monitoring
3. **Backups**: Set up automated database backups
4. **Scaling**: Consider using Docker Swarm or Kubernetes for scaling
5. **Logs**: Use centralized logging (ELK stack, etc.)
