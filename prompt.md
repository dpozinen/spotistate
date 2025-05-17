# Spotistate Project Requirements

## Overview
Spotistate is a web application that allows users to import their library from spotify and store it.

## Features
- User authentication with Spotify. Authentication should be done via UI using oauth to the spotify account.
- Upon authentication, the user should be prompted with an empty home screen with an "import" button in the middle. It should be a Spotify green with the words "IMPORT" on it 
- When clicked the button transforms into a green, circular loading spinner
- On the backend all the user's playlists are fetched and stored into the database
- Once the data is fetched, the UI should show all the imported playlists in a card design. Thumbnails should match those in spotify, but if that is not possible, use a default picture of a spotify logo
- All cards should be clickable. The Contents of the playlist should open on click.
  - It should be sort of a popup/overlay and not cover the whole screen.
  - The content on the sides (the underneath) should be blurred.
  - Top of the ui should be the name of the playlist, clickable, leading to it in Spotify.
  - The track's name, artist, album, length should be shown
  - All tracks in the playlist should be sortable by song name, length, artist, date added.
  - There should be a search bar - searching by name and artist, search should be done on frontend.
  - When clicking on each track it should link to the Spotify track *in that playlist*
  - Clicking anywhere outside should close the overlay

## Technology Stack
- Frontend: React with TypeScript
- Backend: Kotlin, Spring Boot 3
- Database: Postgres SQL for storing imported user data 
- Spotify Web API for retrieving user's listening data

## Project Structure
Please implement the basic structure of the project including:
- Frontend setup with React
- Backend API endpoints + services
- Database models
- Spotify API integration
- Basic UI components

## Additional Notes
- The design should follow Spotify's color scheme
- Focus on desktop, this is not designed for mobile
- Implement proper error handling for API requests
- Remember to animate different actions (like playlist popping up, import button transition to loading etc)
- should be built as a docker container
- use playtika testcontainers for database integration test 