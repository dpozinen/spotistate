package com.spotistate.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import se.michaelthelin.spotify.SpotifyApi
import java.net.URI

@Configuration
class SpotifyConfig {

    @Value("\${spotify.client-id}")
    private lateinit var clientId: String

    @Value("\${spotify.client-secret}")
    private lateinit var clientSecret: String

    @Value("\${spotify.redirect-uri}")
    private lateinit var redirectUri: String

    @Bean
    fun spotifyApi(): SpotifyApi {
        return SpotifyApi.builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRedirectUri(URI.create(redirectUri))
            .build()
    }
}
