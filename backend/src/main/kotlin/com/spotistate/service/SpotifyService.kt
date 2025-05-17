package com.spotistate.service

import com.spotistate.dto.PlaylistDTO
import com.spotistate.dto.TrackDTO
import com.spotistate.dto.UserDTO
import com.spotistate.model.Playlist
import com.spotistate.model.Track
import com.spotistate.model.User
import com.spotistate.repository.PlaylistRepository
import com.spotistate.repository.TrackRepository
import com.spotistate.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack
import se.michaelthelin.spotify.model_objects.specification.SavedTrack
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
class SpotifyService(
    private val spotifyApi: SpotifyApi,
    private val userRepository: UserRepository,
    private val playlistRepository: PlaylistRepository,
    private val trackRepository: TrackRepository
) {
    private val logger = LoggerFactory.getLogger(SpotifyService::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

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
    fun importUserLibrary(userId: String): List<PlaylistDTO> = runBlocking {
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
        
        // Now import playlists asynchronously
        val playlists = spotifyApi.listOfCurrentUsersPlaylists.build().execute()
        
        // Create a deferred task for each playlist
        val deferredPlaylists = playlists.items.map { spotifyPlaylist ->
            coroutineScope.async {
                importPlaylist(spotifyPlaylist, user)
            }
        }
        
        // Wait for all playlist imports to complete and add to the list
        importedPlaylists.addAll(deferredPlaylists.awaitAll())
        
        // Save all playlists in a single transaction
        val savedPlaylists = playlistRepository.saveAll(importedPlaylists)
        
        // Return DTOs
        return@runBlocking savedPlaylists.map { it.toDTO() }
    }
    
    private suspend fun importPlaylist(spotifyPlaylist: PlaylistSimplified, user: User): Playlist {
        logger.info("Importing playlist: {} ({})", spotifyPlaylist.name, spotifyPlaylist.id)
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
        
        // Process tracks
        for (playlistTrack in playlistTracks.items) {
            if (playlistTrack.track == null) continue
            
            playlist.tracks.add(createTrackFromPlaylistTrack(playlistTrack, playlist))
        }
        
        return playlist
    }
    
    private fun createTrackFromPlaylistTrack(playlistTrack: PlaylistTrack, playlist: Playlist): Track {
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
        
        return Track(
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
    }
    
    private suspend fun importLikedSongs(user: User, importedPlaylists: MutableList<Playlist>) {
        try {
            logger.info("Starting to import liked songs for user {}", user.id)
            
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
            
            // Fetch user's saved tracks (liked songs) asynchronously in chunks
            var offset = 0
            val limit = 50
            var hasMore = true
            val trackTasks = mutableListOf<SavedTrack>()
            
            while (hasMore) {
                val savedTracks = spotifyApi.getUsersSavedTracks().limit(limit).offset(offset).build().execute()
                logger.info("Fetched {} liked tracks at offset {}", savedTracks.items.size, offset)
                
                if (savedTracks.items.isEmpty()) {
                    hasMore = false
                } else {
                    trackTasks.addAll(savedTracks.items)
                    offset += limit
                    hasMore = savedTracks.items.size == limit
                }
            }
            
            // Process all tracks
            for (savedTrack in trackTasks) {
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
            
            val trackCount = likedSongsPlaylist.tracks.size
            logger.info("Liked Songs playlist has {} tracks", trackCount)
            
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
