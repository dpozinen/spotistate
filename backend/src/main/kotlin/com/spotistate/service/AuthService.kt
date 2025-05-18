package com.spotistate.service

import com.spotistate.dto.AuthResponseDTO
import com.spotistate.dto.UserDTO
import com.spotistate.model.User
import com.spotistate.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials
import java.util.*

@Service
class AuthService(
    private val spotifyApi: SpotifyApi,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun handleSpotifyCallback(code: String): AuthResponseDTO {
        logger.info("Processing Spotify callback with authorization code")
        
        return try {
            val credentials = getSpotifyCredentials(code)
            logger.info("Successfully obtained Spotify credentials")
            
            spotifyApi.accessToken = credentials.accessToken
            val userProfile = spotifyApi.currentUsersProfile.build().execute()
            logger.info("Successfully fetched user profile for: {}", userProfile.id)
            
            val existingUser = userRepository.findBySpotifyId(userProfile.id)
            val user = if (existingUser != null) {
                logger.info("Updating existing user: {}", userProfile.id)
                existingUser.copy(
                    accessToken = credentials.accessToken,
                    refreshToken = credentials.refreshToken ?: existingUser.refreshToken
                )
            } else {
                logger.info("Creating new user: {}", userProfile.id)
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
            logger.info("Successfully saved user: {}", savedUser.id)
            
            AuthResponseDTO(
                accessToken = credentials.accessToken,
                user = UserDTO(
                    id = savedUser.id,
                    displayName = savedUser.displayName,
                    email = savedUser.email
                )
            )
        } catch (e: Exception) {
            logger.error("Error during Spotify callback processing: {}", e.message, e)
            throw RuntimeException("Failed to process Spotify authentication", e)
        }
    }

    fun getUserInfo(userId: String): UserDTO {
        logger.info("Fetching user info for ID: {}", userId)
        val user = userRepository.findById(userId).orElseThrow { 
            logger.error("User not found with ID: {}", userId)
            RuntimeException("User not found") 
        }
        return UserDTO(
            id = user.id,
            displayName = user.displayName,
            email = user.email
        )
    }

    private fun getSpotifyCredentials(code: String): AuthorizationCodeCredentials {
        logger.info("Exchanging authorization code for access token")
        return try {
            val credentials = spotifyApi.authorizationCode(code).build().execute()
            logger.info("Successfully exchanged authorization code for credentials")
            credentials
        } catch (e: Exception) {
            logger.error("Failed to exchange authorization code: {}", e.message, e)
            when (e) {
                is se.michaelthelin.spotify.exceptions.detailed.BadRequestException -> {
                    logger.error("Bad request error details: message={}", e.message)
                }
                is se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException -> {
                    logger.error("Unauthorized error details: message={}", e.message)
                }
                is se.michaelthelin.spotify.exceptions.detailed.ForbiddenException -> {
                    logger.error("Forbidden error details: message={}", e.message)
                }
                is se.michaelthelin.spotify.exceptions.SpotifyWebApiException -> {
                    logger.error("Spotify API error details: message={}", e.message)
                }
            }
            throw e
        }
    }

    fun refreshToken(userId: String): String {
        logger.info("Refreshing token for user: {}", userId)
        val user = userRepository.findById(userId).orElseThrow { 
            logger.error("User not found for token refresh: {}", userId)
            RuntimeException("User not found") 
        }
        
        return try {
            spotifyApi.refreshToken = user.refreshToken
            val credentials = spotifyApi.authorizationCodeRefresh().build().execute()
            logger.info("Successfully refreshed token for user: {}", userId)
            
            val updatedUser = user.copy(accessToken = credentials.accessToken)
            userRepository.save(updatedUser)
            
            credentials.accessToken
        } catch (e: Exception) {
            logger.error("Failed to refresh token for user {}: {}", userId, e.message, e)
            throw RuntimeException("Failed to refresh token", e)
        }
    }
}
