package com.spotistate.repository

import com.spotistate.model.Track
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TrackRepository : JpaRepository<Track, String> {
    fun findAllByPlaylistId(playlistId: String): List<Track>
}
