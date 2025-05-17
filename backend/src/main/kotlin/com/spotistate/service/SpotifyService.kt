package com.spotistate.service

import com.spotistate.dto.PlaylistDTO
import com.spotistate.dto.TrackDTO
import com.spotistate.dto.UserDTO
import com.spotistate.model.Playlist
import com.spotistate.model.Track
import com.spotistate.repository.PlaylistRepository
import com.spotistate.repository.TrackRepository
import com.spotistate.repository.UserRepository
import org.springframework.stereotype.Service
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.specification.SavedTrack
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import org.springframework.transaction.annotation.Transactional

@Service
class SpotifyService(
    private val spotifyApi: SpotifyApi,
    private val userRepository: UserRepository,
    private val playlistRepository: PlaylistRepository,
    private val trackRepository: TrackRepository
) {

    fun getUserProfile(accessToken: String): UserDTO {
        spotifyApi.accessToken = accessToken
        val userProfile = spotifyApi.currentUsersProfile.build().execute()
        
        return UserDTO(
            id = userProfile.id,
            displayName = userProfile.displayName,
            email = userProfile.email
        )
    }

    @Transactional
    fun importUserLibrary(userId: String): List<PlaylistDTO> {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        spotifyApi.accessToken = user.accessToken
        
        // First, delete all existing playlists and tracks for this user
        playlistRepository.deleteAllByUserId(userId)
        
        val importedPlaylists = mutableListOf<Playlist>()
        
        // Import Liked Songs first
        try {
            importLikedSongs(user, importedPlaylists)
        } catch (e: Exception) {
            throw RuntimeException("Failed to import Liked Songs: ${e.message}", e)
        }
        
        // Now import playlists
        val playlists = spotifyApi.listOfCurrentUsersPlaylists.limit(50).build().execute()
        
        for (spotifyPlaylist in playlists.items) {
            val playlistId = UUID.randomUUID().toString()
            
            val playlist = Playlist(
                id = playlistId,
                spotifyId = spotifyPlaylist.id,
                name = spotifyPlaylist.name,
                description = "", // PlaylistSimplified doesn't have description
                imageUrl = if (spotifyPlaylist.images.isNotEmpty()) spotifyPlaylist.images[0].url else null,
                spotifyUrl = if (spotifyPlaylist.externalUrls != null && spotifyPlaylist.externalUrls.externalUrls.containsKey("spotify"))
                              spotifyPlaylist.externalUrls["spotify"] else "",
                trackCount = spotifyPlaylist.tracks.total,
                user = user
            )
            
            // Fetch tracks for this playlist
            val playlistTracks = spotifyApi.getPlaylistsItems(spotifyPlaylist.id).build().execute()
            for (playlistTrack in playlistTracks.items) {
                if (playlistTrack.track == null) continue
                
                val track = playlistTrack.track as se.michaelthelin.spotify.model_objects.specification.Track
                val trackId = UUID.randomUUID().toString()
                
                // Safely build artist string
                val artistNames = StringBuilder()
                if (track.artists != null && track.artists.isNotEmpty()) {
                    for (i in track.artists.indices) {
                        if (i > 0) artistNames.append(", ")
                        artistNames.append(track.artists[i].name)
                    }
                } else {
                    artistNames.append("Unknown Artist")
                }
                
                // Handle added date
                val addedAt = if (playlistTrack.addedAt != null) {
                    try {
                        LocalDateTime.ofInstant(
                            Instant.parse(playlistTrack.addedAt.toString()),
                            ZoneId.systemDefault()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                
                // Safely get album name
                val albumName = if (track.album != null) track.album.name else "Unknown Album"
                
                // Safely get Spotify URL
                val spotifyUrl = if (track.externalUrls != null && track.externalUrls.externalUrls.containsKey("spotify"))
                                  track.externalUrls["spotify"] else ""
                
                val trackEntity = Track(
                    id = trackId,
                    spotifyId = track.id,
                    name = track.name ?: "Unknown Track",
                    artist = artistNames.toString(),
                    album = albumName,
                    durationMs = track.durationMs,
                    spotifyUrl = spotifyUrl ?: "",
                    addedAt = addedAt,
                    playlist = playlist
                )
                
                playlist.tracks.add(trackEntity)
            }
            
            importedPlaylists.add(playlist)
        }
        
        playlistRepository.saveAll(importedPlaylists)
        
        return importedPlaylists.map { it.toDTO() }
    }
    
    private fun importLikedSongs(user: com.spotistate.model.User, importedPlaylists: MutableList<Playlist>) {
        try {
            println("Starting to import liked songs for user ${user.id}")
            
            // Create a special "Liked Songs" playlist using the constructor that allows setting trackCount later
            val playlistId = UUID.randomUUID().toString()
            val likedSongsPlaylist = Playlist(
                id = playlistId,
                spotifyId = "liked_songs_${user.id}", // Custom ID since it's not a real Spotify playlist
                name = "Liked Songs",
                description = "Your Spotify Liked Songs",
                imageUrl = "https://t.scdn.co/images/3099b3803ad9496896c43f22fe9be8c4.png", // Default Spotify heart icon
                spotifyUrl = "https://open.spotify.com/collection/tracks",
                user = user
            )
            
            // Fetch user's saved tracks (liked songs)
            var offset = 0
            val limit = 50
            var hasMore = true
            
            while (hasMore) {
                val savedTracks = spotifyApi.getUsersSavedTracks().limit(limit).offset(offset).build().execute()
                println("Fetched ${savedTracks.items.size} liked tracks at offset $offset")
                
                if (savedTracks.items.isEmpty()) {
                    hasMore = false
                } else {
                    for (savedTrack in savedTracks.items) {
                        val trackId = UUID.randomUUID().toString()
                        val track = savedTrack.track
                        
                        // Safely build artist string
                        val artistNames = StringBuilder()
                        if (track.artists != null && track.artists.isNotEmpty()) {
                            for (i in track.artists.indices) {
                                if (i > 0) artistNames.append(", ")
                                artistNames.append(track.artists[i].name)
                            }
                        } else {
                            artistNames.append("Unknown Artist")
                        }
                        
                        // Handle added date
                        val addedAt = if (savedTrack.addedAt != null) {
                            try {
                                LocalDateTime.ofInstant(
                                    Instant.parse(savedTrack.addedAt.toString()),
                                    ZoneId.systemDefault()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        
                        // Safely get album name
                        val albumName = if (track.album != null) track.album.name else "Unknown Album"
                        
                        // Safely get Spotify URL
                        val spotifyUrl = if (track.externalUrls != null && track.externalUrls.externalUrls.containsKey("spotify"))
                                          track.externalUrls["spotify"] else ""
                        
                        val trackEntity = Track(
                            id = trackId,
                            spotifyId = track.id,
                            name = track.name ?: "Unknown Track",
                            artist = artistNames.toString(),
                            album = albumName,
                            durationMs = track.durationMs,
                            spotifyUrl = spotifyUrl ?: "",
                            addedAt = addedAt,
                            playlist = likedSongsPlaylist
                        )
                        
                        likedSongsPlaylist.tracks.add(trackEntity)
                    }
                    
                    offset += limit
                    hasMore = savedTracks.items.size == limit
                }
            }
            
            val trackCount = likedSongsPlaylist.tracks.size
            println("Liked Songs playlist has $trackCount tracks")
            
            // Only add if there are liked tracks
            if (trackCount > 0) {
                // Create a new playlist with the correct track count
                val updatedPlaylist = Playlist(
                    id = likedSongsPlaylist.id,
                    spotifyId = likedSongsPlaylist.spotifyId,
                    name = likedSongsPlaylist.name,
                    description = likedSongsPlaylist.description,
                    imageUrl = likedSongsPlaylist.imageUrl,
                    spotifyUrl = likedSongsPlaylist.spotifyUrl,
                    trackCount = trackCount,
                    user = likedSongsPlaylist.user,
                    tracks = likedSongsPlaylist.tracks
                )
                
                importedPlaylists.add(0, updatedPlaylist) // Add at index 0 to make it appear first
            } else {
                throw RuntimeException("No liked songs found")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to import Liked Songs: ${e.message}", e)
        }
    }

    fun getPlaylists(userId: String): List<PlaylistDTO> {
        val playlists = playlistRepository.findAllByUserId(userId)
        return playlists.map { it.toDTO() }
    }

    fun getPlaylistTracks(playlistId: String): List<TrackDTO> {
        val tracks = trackRepository.findAllByPlaylistId(playlistId)
        return tracks.map { it.toDTO() }
    }

    private fun Playlist.toDTO(): PlaylistDTO {
        return PlaylistDTO(
            id = this.id,
            name = this.name,
            description = this.description,
            imageUrl = this.imageUrl,
            spotifyUrl = this.spotifyUrl,
            trackCount = this.tracks.size  // Use actual track count from tracks collection
        )
    }

    private fun Track.toDTO(): TrackDTO {
        return TrackDTO(
            id = this.id,
            name = this.name,
            artist = this.artist,
            album = this.album,
            durationMs = this.durationMs,
            spotifyUrl = this.spotifyUrl,
            addedAt = this.addedAt
        )
    }
}
