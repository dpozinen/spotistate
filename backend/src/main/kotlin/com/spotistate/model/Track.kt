package com.spotistate.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tracks")
data class Track(
    @Id
    val id: String,

    @Column(length = 500)
    val spotifyId: String,
    
    @Column(columnDefinition = "TEXT")
    val name: String,
    
    @Column(columnDefinition = "TEXT")
    val artist: String,
    
    @Column(columnDefinition = "TEXT")
    val album: String,
    
    val durationMs: Int,
    
    @Column(columnDefinition = "TEXT")
    val spotifyUrl: String,
    
    val addedAt: LocalDateTime?,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id")
    val playlist: Playlist
)
