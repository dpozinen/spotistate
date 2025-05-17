package com.spotistate.repository

import com.spotistate.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findBySpotifyId(spotifyId: String): User?
}
