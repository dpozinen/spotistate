package com.spotistate.repository

import com.spotistate.model.Playlist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlaylistRepository : JpaRepository<Playlist, String> {
    fun findAllByUserId(userId: String): List<Playlist>
    fun deleteAllByUserId(userId: String)
}
