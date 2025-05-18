package com.spotistate.controller

import com.spotistate.dto.AuthResponseDTO
import com.spotistate.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import se.michaelthelin.spotify.SpotifyApi
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest
import java.net.URI

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["http://localhost:3000", "http://127.0.0.1:3000"])
class AuthController(
    private val authService: AuthService,
    private val spotifyApi: SpotifyApi
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @GetMapping("/login")
    fun login(): RedirectView {
        try {
            logger.info("Initiating Spotify login process")
            val authorizationCodeUriRequest: AuthorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("user-read-private user-read-email playlist-read-private playlist-read-collaborative user-library-read")
                .show_dialog(true)
                .build()
                
            val uri = authorizationCodeUriRequest.execute()
            logger.info("Redirecting to Spotify authorization: {}", uri.toString())
            return RedirectView(uri.toString())
        } catch (e: Exception) {
            logger.error("Failed to initiate Spotify login: {}", e.message, e)
            throw e
        }
    }

    @GetMapping("/callback")
    fun handleCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) error_description: String?
    ): RedirectView {
        logger.info("Received Spotify callback - code: {}, error: {}, error_description: {}", 
            code?.let { "present" } ?: "null", error, error_description)
        
        return try {
            if (error != null) {
                logger.error("Spotify auth error: {} - {}", error, error_description)
                val frontendErrorUrl = "http://localhost:3000/callback?error=${error}&error_description=${error_description ?: ""}"
                RedirectView(frontendErrorUrl)
            } else if (code != null) {
                val response = authService.handleSpotifyCallback(code)
                logger.info("Successfully authenticated user: {}", response.user.id)
                
                // Create redirect to frontend with token and user data
                val frontendCallbackUrl = "http://localhost:3000/callback?accessToken=${response.accessToken}&userId=${response.user.id}"
                RedirectView(frontendCallbackUrl)
            } else {
                logger.error("No authorization code or error received from Spotify")
                val frontendErrorUrl = "http://localhost:3000/callback?error=invalid_request&error_description=No authorization code received"
                RedirectView(frontendErrorUrl)
            }
        } catch (e: Exception) {
            logger.error("Exception during Spotify callback handling: {}", e.message, e)
            val frontendErrorUrl = "http://localhost:3000/callback?error=server_error&error_description=Authentication failed"
            RedirectView(frontendErrorUrl)
        }
    }

    @GetMapping("/user-info")
    fun getUserInfo(@RequestParam token: String, @RequestParam userId: String): ResponseEntity<AuthResponseDTO> {
        return try {
            logger.info("Fetching user info for user: {}", userId)
            val userInfo = authService.getUserInfo(userId)
            ResponseEntity.ok(AuthResponseDTO(token, userInfo))
        } catch (e: Exception) {
            logger.error("Failed to fetch user info for user {}: {}", userId, e.message, e)
            throw e
        }
    }

    @GetMapping("/refresh/{userId}")
    fun refreshToken(@PathVariable userId: String): ResponseEntity<Map<String, String>> {
        return try {
            logger.info("Refreshing token for user: {}", userId)
            val accessToken = authService.refreshToken(userId)
            ResponseEntity.ok(mapOf("accessToken" to accessToken))
        } catch (e: Exception) {
            logger.error("Failed to refresh token for user {}: {}", userId, e.message, e)
            throw e
        }
    }
}
