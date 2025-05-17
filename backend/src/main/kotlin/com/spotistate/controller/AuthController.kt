package com.spotistate.controller

import com.spotistate.dto.AuthResponseDTO
import com.spotistate.service.AuthService
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
@CrossOrigin(origins = ["http://localhost:3000"])
class AuthController(
    private val authService: AuthService,
    private val spotifyApi: SpotifyApi
) {

    @GetMapping("/login")
    fun login(): RedirectView {
        val authorizationCodeUriRequest: AuthorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
            .scope("user-read-private user-read-email playlist-read-private playlist-read-collaborative user-library-read")
            .show_dialog(true)
            .build()
            
        val uri = authorizationCodeUriRequest.execute()
        return RedirectView(uri.toString())
    }

    @GetMapping("/callback")
    fun handleCallback(@RequestParam code: String): RedirectView {
        val response = authService.handleSpotifyCallback(code)
        
        // Create redirect to frontend with token and user data
        val frontendCallbackUrl = "http://localhost:3000/callback?accessToken=${response.accessToken}&userId=${response.user.id}"
        
        return RedirectView(frontendCallbackUrl)
    }

    @GetMapping("/user-info")
    fun getUserInfo(@RequestParam token: String, @RequestParam userId: String): ResponseEntity<AuthResponseDTO> {
        val userInfo = authService.getUserInfo(userId)
        return ResponseEntity.ok(AuthResponseDTO(token, userInfo))
    }

    @GetMapping("/refresh/{userId}")
    fun refreshToken(@PathVariable userId: String): ResponseEntity<Map<String, String>> {
        val accessToken = authService.refreshToken(userId)
        return ResponseEntity.ok(mapOf("accessToken" to accessToken))
    }
}
