package com.spotistate.dto

import java.time.LocalDateTime

data class UserDTO(
    val id: String,
    val displayName: String?,
    val email: String?
)

data class PlaylistDTO(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val spotifyUrl: String,
    val trackCount: Int
)

data class TrackDTO(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val durationMs: Int,
    val spotifyUrl: String,
    val addedAt: LocalDateTime?
)

data class AuthResponseDTO(
    val accessToken: String,
    val user: UserDTO
)

data class ImportResponseDTO(
    val success: Boolean,
    val playlists: List<PlaylistDTO>
)
