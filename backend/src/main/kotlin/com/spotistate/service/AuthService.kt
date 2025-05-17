package com.spotistate.service

import com.spotistate.dto.AuthResponseDTO
import com.spotistate.dto.UserDTO
import com.spotistate.model.User
import com.spotistate.repository.UserRepository
import org.springframework.stereotype.Service
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials
import java.util.*

@Service
class AuthService(
    private val spotifyApi: SpotifyApi,
    private val userRepository: UserRepository
) {

    fun handleSpotifyCallback(code: String): AuthResponseDTO {
        val credentials = getSpotifyCredentials(code)
        spotifyApi.accessToken = credentials.accessToken
        
        val userProfile = spotifyApi.currentUsersProfile.build().execute()
        
        val existingUser = userRepository.findBySpotifyId(userProfile.id)
        val user = if (existingUser != null) {
            existingUser.copy(
                accessToken = credentials.accessToken,
                refreshToken = credentials.refreshToken ?: existingUser.refreshToken
            )
        } else {
            User(
                id = UUID.randomUUID().toString(),
                spotifyId = userProfile.id,
                email = userProfile.email,
                displayName = userProfile.displayName,
                accessToken = credentials.accessToken,
                refreshToken = credentials.refreshToken ?: ""
            )
        }
        
        val savedUser = userRepository.save(user)
        
        return AuthResponseDTO(
            accessToken = credentials.accessToken,
            user = UserDTO(
                id = savedUser.id,
                displayName = savedUser.displayName,
                email = savedUser.email
            )
        )
    }

    fun getUserInfo(userId: String): UserDTO {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        return UserDTO(
            id = user.id,
            displayName = user.displayName,
            email = user.email
        )
    }

    private fun getSpotifyCredentials(code: String): AuthorizationCodeCredentials {
        return spotifyApi.authorizationCode(code).build().execute()
    }

    fun refreshToken(userId: String): String {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        spotifyApi.refreshToken = user.refreshToken
        
        val credentials = spotifyApi.authorizationCodeRefresh().build().execute()
        
        val updatedUser = user.copy(accessToken = credentials.accessToken)
        userRepository.save(updatedUser)
        
        return credentials.accessToken
    }
}
