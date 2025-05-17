package com.spotistate.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: String,
    
    val spotifyId: String,
    
    val email: String?,
    
    val displayName: String?,
    
    @Column(length = 500)
    val accessToken: String,
    
    @Column(length = 500)
    val refreshToken: String,
    
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val playlists: MutableList<Playlist> = mutableListOf()
)
