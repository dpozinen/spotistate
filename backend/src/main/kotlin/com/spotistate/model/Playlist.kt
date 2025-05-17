package com.spotistate.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "playlists")
data class Playlist(
    @Id
    val id: String,
    
    val spotifyId: String,
    
    @Column(length = 500)
    val name: String,
    
    @Column(length = 2000)
    val description: String?,
    
    @Column(length = 1000)
    val imageUrl: String?,
    
    @Column(length = 1000)
    val spotifyUrl: String,
    
    val trackCount: Int,
    
    val importedAt: LocalDateTime = LocalDateTime.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,
    
    @OneToMany(mappedBy = "playlist", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val tracks: MutableList<Track> = mutableListOf()
) {
    // Secondary constructor for creating a playlist with dynamic track count
    constructor(
        id: String,
        spotifyId: String,
        name: String,
        description: String?,
        imageUrl: String?,
        spotifyUrl: String,
        user: User
    ) : this(
        id = id,
        spotifyId = spotifyId,
        name = name,
        description = description,
        imageUrl = imageUrl,
        spotifyUrl = spotifyUrl,
        trackCount = 0, // Initial track count will be zero
        user = user
    )
    
    // JPA ignores this since it's not a persisted field
    @Transient
    fun getActualTrackCount(): Int = tracks.size
}
