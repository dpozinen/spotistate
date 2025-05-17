package com.spotistate.repository

import com.spotistate.model.Playlist
import com.spotistate.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = [
    "spring.test.database.replace=none",
    "spring.datasource.url=jdbc:tc:postgresql:14:///spotistate"
])
class PlaylistRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var playlistRepository: PlaylistRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun findAllByUserId_shouldReturnUserPlaylists() {
        // Given
        val user = User(
            id = UUID.randomUUID().toString(),
            spotifyId = "spotify-user-id",
            email = "test@example.com",
            displayName = "Test User",
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )
        
        entityManager.persist(user)
        
        val playlist1 = Playlist(
            id = UUID.randomUUID().toString(),
            spotifyId = "spotify-playlist-1",
            name = "Playlist 1",
            description = "Description 1",
            imageUrl = "https://example.com/image1.jpg",
            spotifyUrl = "https://spotify.com/playlist/1",
            trackCount = 10,
            importedAt = LocalDateTime.now(),
            user = user
        )
        
        val playlist2 = Playlist(
            id = UUID.randomUUID().toString(),
            spotifyId = "spotify-playlist-2",
            name = "Playlist 2",
            description = "Description 2",
            imageUrl = "https://example.com/image2.jpg",
            spotifyUrl = "https://spotify.com/playlist/2",
            trackCount = 20,
            importedAt = LocalDateTime.now(),
            user = user
        )
        
        entityManager.persist(playlist1)
        entityManager.persist(playlist2)
        entityManager.flush()
        
        // When
        val playlists = playlistRepository.findAllByUserId(user.id)
        
        // Then
        assertEquals(2, playlists.size)
        assertEquals("Playlist 1", playlists[0].name)
        assertEquals("Playlist 2", playlists[1].name)
    }
}
