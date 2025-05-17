# Spotistate

Spotistate is a web application that allows users to import their library from Spotify and store it for reference.

## Features

- User authentication with Spotify using OAuth
- Import user's playlists from Spotify
- View all imported playlists with their thumbnails
- Browse playlist contents with sorting capabilities
- Search tracks by name or artist
- Direct links to Spotify for playlists and tracks

## Technology Stack

- **Frontend**: React with TypeScript and Styled Components
- **Backend**: Kotlin with Spring Boot 3
- **Database**: PostgreSQL for storing imported user data
- **API Integration**: Spotify Web API
- **Containerization**: Docker and Docker Compose

## Project Structure

The project is organized into two main components:

### Backend

- Written in Kotlin using Spring Boot 3
- Provides RESTful API endpoints for authentication and data retrieval
- Integrates with Spotify Web API using spotify-web-api-java library
- Stores imported data in PostgreSQL database
- Includes integration tests using Testcontainers

### Frontend

- Built with React, TypeScript, and Styled Components
- Uses React Router for navigation
- Features responsive design with Spotify-inspired UI
- Implements client-side filtering and sorting

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Spotify Developer Account (for API credentials)

### Configuration

1. Clone the repository
2. Create a Spotify Developer Application to get your Client ID and Secret
3. Configure environment variables:
   - `SPOTIFY_CLIENT_ID`: Your Spotify application client ID
   - `SPOTIFY_CLIENT_SECRET`: Your Spotify application client secret

### Running the Application

```bash
# Start the application with Docker Compose
docker-compose up -d

# The application will be available at:
# - Frontend: http://localhost:3000
# - Backend API: http://localhost:8080
```

## Development

### Backend Development

```bash
cd backend
./gradlew bootRun
```

### Frontend Development

```bash
cd frontend
npm install
npm start
```

## Testing

```bash
# Run backend tests
cd backend
./gradlew test

# Run frontend tests
cd frontend
npm test
```

## Deployment

The application is containerized and can be deployed to any container orchestration platform:

- Build the Docker images: `docker-compose build`
- Push the images to your container registry
- Deploy using Kubernetes, AWS ECS, or similar services
